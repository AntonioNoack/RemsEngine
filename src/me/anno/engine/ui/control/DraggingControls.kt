package me.anno.engine.ui.control

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.*
import me.anno.ecs.prefab.change.Path
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Touch
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.length
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.ui.input.EnumInput
import me.anno.utils.Warning.unused
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Hierarchical
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Floats.toDegrees
import org.apache.logging.log4j.LogManager
import org.joml.Planed
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.sign
import kotlin.math.tan

// done controls
// done show the scene
// done drag stuff
// done translate, rotate, scale with gizmos
// todo gizmos & movement for properties with @PositionAnnotation

// advanced snapping
// todo mode to place it on top of things using mesh bounds
// todo xyz keys to rotate 90° on that axis
// todo shift for dynamic angles

// todo rotate base grid for working at angles?

// automatically attach it to that object, that is being targeted? mmh.. no, use the selection for that
// done draw the gizmos

open class DraggingControls(renderView: RenderView) : ControlScheme(renderView) {

    var mode = Mode.TRANSLATING

    // todo mode where hanging plants are automatically rotated to match a wall

    // todo apply snapping to <when moving things around> as well
    // todo we could snap rotations, and maybe scale, as well

    val drawModeInput = EnumInput(
        "Draw Mode", "", renderView.renderMode.name, RenderMode.values.map { NameDesc(it.name) }, style
    ).setChangeListener { _, index, _ ->
        renderView.renderMode = RenderMode.values[index]
    }

    val snappingSettings = SnappingSettings()

    init {
        // todo debugging view selection
        val topLeft = PanelListX(style)
        topLeft.add(drawModeInput)
        topLeft.add(TextButton("Play", "Start the game", false, style)
            .addLeftClickListener {
                ECSSceneTabs.currentTab?.play()
                // todo also set this instance text to "Back"
            })
        /* todo this class isn't used in play-testing...
        val pauseButton = TextButton("Pause", "", false, style).addLeftClickListener {
            // pause/unpause
            Time.timeSpeed = if (Time.timeSpeed == 0.0) 1.0 else 0.0
            (it as TextButton).text = if (Time.timeSpeed == 0.0) "Unpause" else "Pause"
        }
        val restartButton = TextButton("Restart", "", false, style).addLeftClickListener {
            view.getWorld()?.prefab?.invalidateInstance()
        }
        topLeft.add(pauseButton)
        topLeft.add(restartButton)
        topLeft.add(SpyPanel {
            val isPlayTesting = view.playMode == PlayMode.PLAY_TESTING
            pauseButton.isVisible = isPlayTesting
            restartButton.isVisible = isPlayTesting
        })
        */
        topLeft.add(TextButton("Snap", style)
            .addLeftClickListener {
                EditorState.select(snappingSettings)
            })
        add(topLeft)
    }

    override fun onUpdate() {
        super.onUpdate()
        if (dragged != null) invalidateDrawing() // might be displayable
        drawModeInput.setValue(NameDesc(renderView.renderMode.name), false)
    }

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        val dragged = dragged
        if (dragged != null && isHovered) {
            // if something is dragged, draw it as a preview
            if (dragged.getContentType() == "File") {
                val original = dragged.getOriginal()
                val files = if (original is FileReference) listOf(original)
                else (original as List<*>).filterIsInstance<FileReference>()
                val pos = JomlPools.vec3d.create()
                for (file in files) {

                    // to do another solution would be to add it temporarily to the hierarchy, and remove it if it is cancelled

                    val prefab = PrefabCache[file] ?: continue
                    // todo sdf component, mesh component, light components and colliders would be fine as well
                    val sample = prefab.createInstance() // a little waste of allocations...

                    // find where to draw it
                    findDropPosition(file, pos)

                    movedSample.removeAllChildren()
                    movedSample.transform.localPosition = pos
                    when (sample) {
                        is Component -> movedSample.add(sample)
                        is Entity -> movedSample.add(sample)
                        // else ...
                    }
                    movedSample.validateTransform()
                    if (sample is Entity) sample.validateTransform()

                    pipeline.fill(movedSample)

                    // draw its gui
                    // todo add them as normal meshes instead
                    // view.drawGizmos(sample, pos, false)
                }
                JomlPools.vec3d.sub(1)
            }
        }
    }

    val movedSample = Entity()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // show the mode
        drawSimpleTextCharByChar(
            x + width - 2 - DrawTexts.monospaceFont.sampleWidth,
            y + height - 2 - DrawTexts.monospaceFont.sampleHeight,
            2, when (mode) {
                Mode.TRANSLATING -> "T"
                Mode.ROTATING -> "R"
                Mode.SCALING -> "S"
                else -> "X"
            }
        )
    }

    override fun drawGizmos() {
        GFXState.depthMode.use(DepthMode.ALWAYS) {
            GFXState.depthMask.use(false) {
                drawGizmos2()
            }
        }
    }

    private var gizmoMask: Int = 0
    private fun drawGizmos2() {
        val md = renderView.getMouseRayDirection()
        val chosenId = when (gizmoMask) {
            // single axis
            1 -> 0
            2 -> 1
            4 -> 2
            // multiple axes
            3 -> 5
            5 -> 4
            6 -> 3
            else -> -1
        }
        var newGizmoMask = 0
        val pos = JomlPools.vec3d.create()
        for (selected0 in EditorState.selection) {
            var selected: PrefabSaveable? = selected0 as? PrefabSaveable
            while (selected != null && selected !is Entity) {
                selected = selected.parent
            }
            // todo gizmos for sdf components
            // todo like Unity allow more gizmos than that?
            if (selected is Entity) {
                val scale = renderView.radius * 0.15
                val transform = selected.transform.globalTransform
                transform.getTranslation(pos)
                val cam = renderView.cameraMatrix
                val mask = when (mode) {
                    Mode.TRANSLATING -> Gizmos.drawTranslateGizmos(cam, pos, scale, 0, chosenId, md)
                    Mode.ROTATING -> Gizmos.drawRotateGizmos(cam, pos, scale, 0, chosenId, md)
                    Mode.SCALING -> Gizmos.drawScaleGizmos(cam, pos, scale, 0, chosenId, md)
                    Mode.NOTHING -> 0
                }
                if (mask != 0 && Input.mouseKeysDown.isEmpty()) {
                    newGizmoMask = mask
                }
            }
        }
        if (Input.mouseKeysDown.isEmpty()) {
            gizmoMask = newGizmoMask
        }
        JomlPools.vec3d.sub(1)
    }

    open fun resetCamera() {
        // reset the camera
        renderView.rotation.identity()
        renderView.position.set(0.0)
        renderView.radius = 50.0
        renderView.near = 1e-3
        renderView.far = 1e10
        camera.fovOrthographic = 5f
        camera.fovY = 90f
        camera.isPerspective = true
        rotationTarget.set(0.0)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "SetMode(MOVE)" -> mode = Mode.TRANSLATING
            "SetMode(ROTATE)" -> mode = Mode.ROTATING
            "SetMode(SCALE)" -> mode = Mode.SCALING
            "Cam0", "ResetCamera" -> resetCamera()
            "Cam5" -> {// switch between orthographic and perspective
                // switch between ortho and perspective
                camera.isPerspective = !camera.isPerspective
            }
            // todo control + numpad does not work
            "Cam1" -> rotateCameraTo(Vector3f(0f, if (Input.isControlDown) 180f else 0f, 0f))// default view
            "Cam3" -> rotateCameraTo(Vector3f(0f, if (Input.isControlDown) -90f else +90f, 0f))// rotate to side view
            "Cam7" -> rotateCameraTo(Vector3f(if (Input.isControlDown) +90f else -90f, 0f, 0f)) // look from above
            "Cam4" -> rotateCamera(
                if (Input.isShiftDown) {// left
                    Vector3f(0f, 0f, -15f)
                } else {
                    Vector3f(0f, -15f, 0f)
                }
            )
            "Cam6" -> rotateCamera(
                if (Input.isShiftDown) {// right
                    Vector3f(0f, 0f, +15f)
                } else {
                    Vector3f(0f, +15f, 0f)
                }
            )
            "Cam8" -> rotateCamera(Vector3f(-15f, 0f, 0f)) // up
            "Cam2" -> rotateCamera(Vector3f(+15f, 0f, 0f)) // down
            "Cam9" -> rotateCamera(Vector3f(0f, 180f, 0f)) // look at back; rotate by 90 degrees on y axis
            // todo use this for moving around
            /*"MoveLeft" -> this.inputDx--
            "MoveRight" -> this.inputDx++
            "MoveUp" -> this.inputDy++
            "MoveDown" -> this.inputDy--
            "MoveForward" -> this.inputDz--
            "MoveBackward", "MoveBack" -> this.inputDz++*/
            /*"Turn" -> turn(dx, dy)
            "TurnLeft" -> turn(-1f, 0f)
            "TurnRight" -> turn(1f, 0f)
            "TurnUp" -> turn(0f, -1f)
            "TurnDown" -> turn(0f, 1f)*/
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    @NotSerializedProperty
    val snapRemainder = Vector3d()

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (EditorState.control?.onMouseMoved(x, y, dx, dy) == true) return
        if (EditorState.editMode?.onEditMove(x, y, dx, dy) == true) return
        if (Touch.touches.size > 1) return // handled separately
        // super.onMouseMoved(x, y, dx, dy)
        val mode = mode
        when {
            isSelected && Input.isRightDown -> {
                rotateCamera(dx, dy)
            }
            isSelected && Input.isMiddleDown -> {
                // move camera
                val fovYRadians = renderView.editorCamera.fovY
                val speed = tan(fovYRadians * 0.5) * renderView.radius / height
                val camTransform = camera.transform!!
                val globalCamTransform = camTransform.globalTransform
                val offset = globalCamTransform.transformDirection(Vector3d(dx * speed, -dy * speed, 0.0))
                renderView.position.sub(offset)
                camera.invalidateAABB()
            }
            isSelected && Input.isLeftDown && mode != Mode.NOTHING -> {

                val selection = EditorState.selection
                if (selection.isEmpty()) return
                val prefab = selection.firstInstanceOrNull<PrefabSaveable>()?.root?.prefab ?: return
                if (!prefab.isWritable) {
                    LOGGER.warn("Prefab from '${prefab.source}' cannot be directly modified, inherit from it")
                    return
                }

                /**
                 * drag the selected object
                 **/
                // for that transform dx,dy into global space,
                // and then update the local space
                val fovYRadians = renderView.editorCamera.fovY
                val speed = tan(fovYRadians * 0.5) / height
                val camTransform = camera.transform!!.globalTransform
                val offset = JomlPools.vec3d.create()
                offset.set(dx * speed, -dy * speed, 0.0)
                camTransform.transformDirection(offset)

                val gizmoMask = gizmoMask
                if (gizmoMask != 0) {
                    if (!gizmoMask.hasFlag(1)) offset.x = 0.0
                    if (!gizmoMask.hasFlag(2)) offset.y = 0.0
                    if (!gizmoMask.hasFlag(4)) offset.z = 0.0
                }

                // rotate around the direction
                // we could use the average mouse position as center; this probably would be easier
                val dir = renderView.getMouseRayDirection()
                val rx = (x - (this.x + this.width * 0.5)) // [-w/2,+w/2]
                val ry = (y - (this.y + this.height * 0.5)) // [-h/2,+h/2]
                val rotationAngle = PI * (rx * dy - ry * dx) / (length(rx, ry) * height)

                val targets2 = selectedMovables
                val targets3 = selectedEntities + targets2
                // remove all targets of which there is a parent selected
                targets3.filter { target ->
                    val loh = (target as? Hierarchical<*>)?.listOfHierarchy?.toHashSet() ?: emptySet()
                    targets3.none2 {
                        it !== target && it in loh
                    }
                }
                if (targets3.isNotEmpty()) {
                    for (index in targets3.indices) {// for correct transformation when parent and child are selected together
                        when (val inst = targets3[index]) {
                            is Entity -> {
                                val transform = inst.transform
                                when (mode) {
                                    Mode.TRANSLATING -> {
                                        val global = transform.globalTransform
                                        val distance = camTransform.distance(global)
                                        if (distance > 0.0) {
                                            // correct
                                            offset.mul(distance)
                                            val snap = snappingSettings
                                            if (snap.snapX || snap.snapY || snap.snapZ) {
                                                snappingSettings.applySnapping(offset, snapRemainder)
                                            }
                                            global.translateLocal(offset)
                                            offset.div(distance)
                                        }
                                        transform.invalidateLocal()
                                    }
                                    Mode.ROTATING -> {
                                        // todo rotate rotating gizmo
                                        when (gizmoMask) {
                                            1 -> transform.localRotation = transform.localRotation
                                                .toEulerAnglesDegrees()
                                                .apply {
                                                    this.x += rotationAngle * sign(dir.x).toDegrees()
                                                }.toQuaternionDegrees()
                                            2 -> transform.localRotation = transform.localRotation
                                                .toEulerAnglesDegrees()
                                                .apply {
                                                    this.y += rotationAngle * sign(dir.y).toDegrees()
                                                }.toQuaternionDegrees()
                                            4 -> transform.localRotation = transform.localRotation
                                                .toEulerAnglesDegrees()
                                                .apply {
                                                    this.z += rotationAngle * sign(dir.z).toDegrees()
                                                }.toQuaternionDegrees()
                                            else -> {
                                                val global = transform.globalTransform
                                                val tmpQ = JomlPools.quat4d.borrow()
                                                tmpQ.fromAxisAngleRad(dir.x, dir.y, dir.z, rotationAngle)
                                                global.rotate(tmpQ)// correct
                                                transform.invalidateLocal()
                                            }
                                        }
                                    }
                                    Mode.SCALING -> {
                                        // todo rotate scaling gizmo
                                        if (gizmoMask == 0) {
                                            val scale = pow(2.0, (dx - dy).toDouble() / height)
                                            transform.localScale = transform.localScale.mul(scale)
                                        } else {
                                            val base = 8.0
                                            val sx = pow(base, offset.x)
                                            val sy = pow(base, offset.y)
                                            val sz = pow(base, offset.z)
                                            transform.localScale = transform.localScale.mul(sx, sy, sz)
                                        }
                                    }
                                    else -> throw NotImplementedError()
                                }
                                transform.teleportUpdate()
                                onChangeTransform(inst)
                            }
                            is DCMovable -> inst.move(this, camTransform, offset, dir, rotationAngle, dx, dy)
                        }
                    }
                    JomlPools.vec3d.sub(1)
                }
            }
        }
    }

    val selectedMovables
        get() = EditorState.selection.mapNotNull {
            when (it) {
                is DCMovable -> it
                is Hierarchical<*> -> it.parent as? DCMovable
                else -> null
            }
        }

    private fun onChangeTransform(entity: Entity) {
        val root = entity.getRoot(Entity::class)
        val prefab = root.prefab
        val path = entity.prefabPath
        val transform = entity.transform
        onChangeTransform1(entity, prefab, path, transform.localPosition, transform.localRotation, transform.localScale)
    }

    fun onChangeTransform1(
        entity: Entity?, prefab: Prefab?, path: Path,
        position: Any, rotation: Any, scale: Any
    ) {
        // save changes to file
        if (prefab != null) {
            prefab[path, "position"] = position
            prefab[path, "rotation"] = rotation
            prefab[path, "scale"] = scale
        }
        if (entity != null) {
            entity.invalidateAABBsCompletely()
            entity.invalidateChildTransforms()
        }
        invalidateInspector()
        PrefabInspector.currentInspector?.onChange(false)
    }

    fun addToParent(
        prefab: Prefab,
        root: PrefabSaveable,
        type: Char,
        position: Any?,
        results: MutableCollection<PrefabSaveable>
    ) {
        val ci = PrefabInspector.currentInspector!!
        val path = ci.addNewChild(root, type, prefab)!!
        if (position != null) ci.prefab[path, "position"] = position
        val sample = Hierarchy.getInstanceAt(ci.root, path)
        if (sample != null) results.add(sample)
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val hovered by lazy { // get hovered element
            renderView.resolveClick(x, y)
        }
        val hovEntity = hovered.first
        val hovComponent = hovered.second
        val results = ArrayList<PrefabSaveable>()
        val ci = PrefabInspector.currentInspector!!
        for (file in files) {
            val prefab = PrefabCache[file] ?: continue
            val dropPosition = findDropPosition(file, Vector3d())
            when (val sampleInstance = prefab.getSampleInstance()) {
                is Material -> {
                    when (hovComponent) {
                        is DCPaintable -> hovComponent.paint(this, sampleInstance, file)
                        is MeshComponentBase -> {
                            val mesh = hovComponent.getMeshOrNull()
                            val numMaterials = mesh?.numMaterials ?: 1
                            if (numMaterials <= 1) {
                                // assign material
                                hovComponent.materials = listOf(file)
                                hovComponent.prefab?.set(hovComponent, "materials", hovComponent.materials)
                            } else {
                                // ask for slot to place material
                                Menu.openMenu(
                                    windowStack, NameDesc("Destination Slot for ${file.nameWithoutExtension}?"),
                                    (0 until numMaterials).map { i ->
                                        MenuOption(NameDesc("$i", "", "")) {
                                            hovComponent.materials = Array(numMaterials) {
                                                if (it == i) file
                                                else hovComponent.materials.getOrNull(it) ?: InvalidRef
                                            }.toList()
                                            hovComponent.prefab?.set(
                                                hovComponent,
                                                "materials",
                                                hovComponent.materials
                                            )
                                        }
                                    }
                                )
                                // what if there are multiple materials being dragged? :); we ask multiple times 😅
                                // todo find what material is used at that triangle :), maybe draw component ids + material ids for this
                            }
                        }
                        else -> {}
                    }
                    // todo if the prefab is not writable, create a prefab for that mesh, and replace the mesh...
                    /*if (mesh != null) {
                        mesh.materials = listOf(file)
                        // add this change
                        val inspector = PrefabInspector.currentInspector
                        if (inspector != null) {
                            val path = mesh.pathInRoot2(inspector.root, true)
                            // inject the mesh into the path;
                            path.setLast(mesh.name, 0, 'm')
                            inspector.change(path, "materials", mesh.materials)
                        }
                    }*/
                }
                is Entity -> {
                    // add this to the scene
                    // where? selected / root
                    // done while dragging this, show preview
                    // done place it where the preview was drawn
                    val root = EditorState.selection.firstInstanceOrNull<Entity>() ?: renderView.getWorld()
                    if (root is Entity) {
                        val position = Vector3d(sampleInstance.transform.localPosition)
                        position.add(dropPosition)
                        addToParent(prefab, root, 'c', position, results)
                        // todo position isn't properly persisted
                        //  - after each save, it jumps back to the center...,
                        //  - when moved it goes to the proper position
                        // todo tree view is also not properly updated... needs redraw...
                        // todo also ECSTreeView reordering isn't working properly...
                        // TreeViews need to be updated
                        for (window in GFX.windows) {
                            for (window1 in window.windowStack) {
                                window1.panel.forAllVisiblePanels {
                                    if (it is ECSTreeView) it.invalidateLayout()
                                }
                            }
                        }
                    } else LOGGER.warn("Could not drop $file onto ${root?.className}")
                }
                is DCDroppable -> sampleInstance.drop(this, prefab, hovEntity, hovComponent, dropPosition, results)
                is Component -> {
                    if (hovEntity != null) {
                        val path = ci.addNewChild(hovEntity, 'c', prefab)!!
                        val sample = Hierarchy.getInstanceAt(ci.root, path)
                        if (sample != null) results.add(sample)
                    }
                }
                else -> {
                    // todo try to add it to all available, hovered and selected instances
                }
            }
            LOGGER.info("pasted $file")
        }
        if (results.isNotEmpty()) {
            EditorState.selection = results
            EditorState.lastSelection = results.last()
            dragged = null
            requestFocus(true) // because we dropped sth here
        }
    }

    fun findDropPosition(drop: FileReference, dst: Vector3d): Vector3d {
        unused(drop)
        // val prefab = PrefabCache[drop] ?: return null
        // val sample = prefab.getSampleInstance() as? Entity ?: return null
        // todo depending on mode, use other strategies to find zero-point on object
        // to do use mouse wheel to change height? maybe...
        // done depending on settings, we also can use snapping
        val cp = renderView.cameraPosition
        val cd = renderView.mouseDirection
        val plane = Planed(0.0, 1.0, 0.0, 0.0)
        var distance = (plane.dot(cd) - plane.dot(cp)) / plane.dot(cd)
        val world = renderView.getWorld()
        if (world is Entity) {
            val query = RayQuery(cp, cd, 1e9)
            val cast = Raycast.raycastClosestHit(world, query)
            if (cast) distance = query.result.distance
        }
        // to do camDirection will only be correct, if this was the last drawn instance
        dst.set(renderView.mouseDirection).mul(distance).add(renderView.cameraPosition)
        snappingSettings.applySnapping(dst)
        return dst
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        super.onPaste(x, y, data, type)
        LOGGER.info("pasted $data/$type")
    }

    override fun onDeleteKey(x: Float, y: Float) {
        for (child in EditorState.selection) {
            if (child is PrefabSaveable) {
                val parent = child.parent
                if (parent != null) {
                    Hierarchy.removePathFromPrefab(parent.root.prefab!!, child)
                }
            }
        }
        // remove it from the selection
        EditorState.selection = emptyList()
    }

    val blenderAddon = BlenderControlsAddon()

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (!blenderAddon.onCharTyped(x, y, codepoint)) super.onCharTyped(x, y, codepoint)
    }

    override fun onEscapeKey(x: Float, y: Float) {
        if (!blenderAddon.onEscapeKey(x, y)) super.onEscapeKey(x, y)
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        if (!blenderAddon.onBackSpaceKey(x, y)) super.onBackSpaceKey(x, y)
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (!blenderAddon.onEnterKey(x, y)) super.onEnterKey(x, y)
    }

    override fun isKeyInput() = true
    override fun acceptsChar(char: Int) = true

    override val className: String get() = "SceneView"

    companion object {
        private val LOGGER = LogManager.getLogger(DraggingControls::class)
    }
}