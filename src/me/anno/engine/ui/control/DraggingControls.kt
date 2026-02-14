package me.anno.engine.ui.control

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.invalidatePhysicsTransform
import me.anno.ecs.EntityQuery.forAllEntitiesInChildren
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.drawing.DrawRectangles.drawBorder
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Key
import me.anno.input.Touch
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.UIColors
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Hierarchical
import me.anno.utils.structures.lists.Lists.castToList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.tan

// todo rectangle-selection on shift-drag

// todo for physics controlled objects, what do we need to make it possible to make them float? remember reference position?

// todo adjust movement speed and mouse rotation based on FOV

// todo select a bunch of assets, make it a collection,
//  and draw them using a brush, for foliage

// todo it would be nice if we could place images onto materials (?)

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

    override val settings = DraggingControlSettings()
    val snappingSettings get() = settings.snapSettings

    init {
        val topLeft = PanelListX(style)
        val topLeftSett = PanelListY(style)
        topLeft.add(topLeftSett)
        topLeft.add(createPlayButton())
        topLeft.add(createSettingsButton())
        topLeft.add(createRenderModeButton())
        topLeft.alignmentX = AxisAlignment.MIN
        topLeft.alignmentY = AxisAlignment.MIN
        add(topLeft)
    }

    private fun createPlayButton(): Panel {
        return TextButton(NameDesc("Play", "Start the game", ""), style)
            .addLeftClickListener { ECSSceneTabs.currentTab?.play() }
    }

    private fun createSettingsButton(): Panel {
        return TextButton(NameDesc("⚙", "Settings", ""), 1f, style)
            .addLeftClickListener { EditorState.select(settings) }
    }

    private fun createRenderModeButton(): Panel {
        val renderSettings = DraggingControlSettings()
        val renderMode0 = renderSettings.renderMode
        return TextButton(renderMode0.nameDesc, style)
            .addLeftClickListener { button ->
                Menu.openMenu(windowStack, RenderMode.values.map { renderMode ->
                    MenuOption(renderMode.nameDesc) {
                        button as TextButton
                        button.text = renderMode.nameDesc.name
                        button.tooltip = renderMode.nameDesc.desc
                        renderView.renderMode = renderMode
                        settings.renderMode = renderMode
                    }
                })
            }
    }

    override fun onUpdate() {
        super.onUpdate()
        updateFromRenderSettings()
        updateGridFromSettings()
        checkAlwaysActiveControls()
    }

    fun updateFromRenderSettings() {
        val renderSettings = settings
        renderView.renderMode = renderSettings.renderMode
        renderView.superMaterial = renderSettings.superMaterial
    }

    fun updateGridFromSettings() {
        val gs = settings
        renderView.drawGridWhenEditing = gs.showGridXY.toInt(1) +
                gs.showGridXZ.toInt(2) +
                gs.showGridYZ.toInt(4)
    }

    fun checkAlwaysActiveControls() {
        if (isAnyChildInFocus || (isHovered && dragged != null)) {
            when {
                Input.wasKeyTyped(Key.KEY_COMMA) -> commaPressLike(-1f)
                Input.wasKeyTyped(Key.KEY_PERIOD) -> commaPressLike(+1f)
                Input.wasKeyTyped(Key.KEY_0) -> {
                    resetDropTransform()
                    // todo apply transform onto selected
                    LOGGER.info("Reset transform")
                }
            }
        }
    }

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        val dragged = dragged
        if (dragged != null && isHovered) {
            // if something is dragged, draw it as a preview
            if (dragged.getContentType() == "File") {

                val original = dragged.getOriginal()
                val files = if (original is FileReference) listOf(original)
                else original.castToList(FileReference::class)
                val dropPosition = JomlPools.vec3d.create()
                for (file in files) {

                    // to do another solution would be to add it temporarily to the hierarchy, and remove it if it is cancelled

                    // todo sdf component, mesh component, light components and colliders would be fine as well
                    val sample = PrefabCache.newInstance(file).waitFor() ?: continue // a little waste of allocations...

                    // find where to draw it
                    findDropPosition(dropPosition)

                    movedSample.removeAllChildren()
                    movedSample.transform.localPosition = dropPosition
                    movedSample.transform.localRotation = dropRotation
                    movedSample.transform.localScale = dropScale
                    when (sample) {
                        is Component -> movedSample.add(sample)
                        is Entity -> movedSample.add(sample)
                        // else ...
                    }

                    // todo as soon as a tab with that prefab was opened,
                    //  motion no longer works :(
                    movedSample.validateTransform()
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

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        drawRectangleSelection()
        drawStats()
        drawModeName()
    }

    private fun getModeName(): String {
        return when (mode) {
            Mode.TRANSLATING -> "T"
            Mode.ROTATING -> "R"
            Mode.SCALING -> "S"
            else -> "X"
        }
    }

    fun drawModeName() {
        drawText(
            x + width, y + height, 2, getModeName(),
            AxisAlignment.MAX, AxisAlignment.MAX
        )
    }

    fun drawRectangleSelection() {
        if (!Input.isLeftDown || !Input.isShiftDown || !isAnyChildInFocus) return
        val window = window ?: return
        val x0 = max(min(window.mouseX, window.mouseDownX).toIntOr(), x)
        val x1 = min(max(window.mouseX, window.mouseDownX).toIntOr(), x + width)
        val y0 = max(min(window.mouseY, window.mouseDownY).toIntOr(), y)
        val y1 = min(max(window.mouseY, window.mouseDownY).toIntOr(), y + height)
        drawBorder(x0, y0, x1 - x0, y1 - y0, UIColors.greenYellow, 2)
    }

    override fun drawGizmos() {
        GFXState.depthMode.use(alwaysDepthMode) {
            GFXState.depthMask.use(false) {
                drawGizmos2()
            }
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (dragged != null) {
            val delta = dx - dy
            mouseWheelLike(delta)
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    fun mouseWheelLike(delta: Float) {
        if (Input.isShiftDown) {
            dropScale.mul(pow(1.05f, delta))
            return
        }
        val snapR = snappingSettings.snapR
        val delta1 = (if (snapR > 0.0) sign(delta) * snapR else delta)
        when {
            Input.isKeyDown(Key.KEY_X) -> dropEuler.x += delta1
            Input.isKeyDown(Key.KEY_Z) -> dropEuler.z += delta1
            else -> dropEuler.y += delta1
        }
        validateDropRotation()
    }

    fun validateDropRotation() {
        snappingSettings.snapRotation(dropEuler, snapRotRem)
        dropEuler.toQuaternionDegrees(dropRotation)
    }

    fun resetDropTransform() {
        dropRotation.identity()
        dropEuler.set(0.0)
        dropScale.set(1.0)
    }

    fun commaPressLike(sign: Float) {
        val snapR = snappingSettings.snapR
        val angle = sign * (if (snapR > 0f) snapR else 45f)
        if (dragged != null) {
            dropEuler.y += angle
            validateDropRotation()
        } else {
            val angle1 = angle.toRadians()
            val dir = Vector3f(0f, 1f, 0f)
            for (inst in instancesToTransform) {
                when (inst) {
                    is Entity -> {
                        val transform = inst.transform
                        transformRotation(transform, angle1, dir, 0)
                        transform.teleportUpdate()
                        onChangeTransform(inst)
                    }
                    // todo dc-moveables
                }
            }
            // todo else apply transform onto selected
        }
    }

    private var gizmoMask: Int = 0
    private fun drawGizmos2() {
        val md = renderView.mouseDirection
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
        val rot = JomlPools.quat4f.create()
        val scale = renderView.radius * tan(camera.fovYDegrees.toRadians() * 0.5f) * 0.2f
        val cam = renderView.cameraMatrix
        val pip = renderView.pipeline
        for (selected in instancesToTransform) {
            // todo like Unity allow more gizmos than that?
            val transform = getTransformMatrix(selected) ?: continue
            transform.getTranslation(pos)
            transform.getUnnormalizedRotation(rot)
            val mask = when (mode) {
                Mode.TRANSLATING -> Gizmos.drawTranslateGizmos(pip, cam, pos, scale, 0, chosenId, md)
                Mode.ROTATING -> Gizmos.drawRotateGizmos(pip, cam, pos, scale, 0, chosenId, md)
                Mode.SCALING -> Gizmos.drawScaleGizmos(pip, cam, pos, rot, scale, 0, chosenId, md)
                Mode.NOTHING -> 0
            }
            if (mask != 0 && Input.mouseKeysDown.isEmpty()) {
                newGizmoMask = mask
            }
        }
        if (Input.mouseKeysDown.isEmpty()) {
            gizmoMask = newGizmoMask
        }
        JomlPools.vec3d.sub(1)
        JomlPools.quat4f.sub(1)
    }

    private fun getTransformMatrix(selected: Any?): Matrix4x3? {
        return when (selected) {
            is Entity -> selected.transform.globalTransform
            is DCMovable -> selected.getGlobalTransform(Matrix4x3())
            else -> null
        }
    }

    open fun resetCamera() {
        // reset the camera
        renderView.orbitRotation.identity()
        renderView.orbitCenter.set(0.0)
        renderView.radius = 10f
        renderView.near = 1e-3f
        renderView.far = 1e10f
        camera.fovOrthographic = 5f
        camera.fovYDegrees = 90f
        camera.isPerspective = true
        rotationTargetDegrees.set(0.0)
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
                camera.fovOrthographic = renderView.radius
            }
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
    val snapPosRem = Vector3d()

    @NotSerializedProperty
    val snapRotRem = Vector3f()

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (EditorState.control?.onMouseMoved(x, y, dx, dy) == true) return
        if (EditorState.editMode?.onEditMove(x, y, dx, dy) == true) return
        if (Touch.touches.size > 1) return // handled separately

        val mode = mode
        when {
            isSelected && Input.isRightDown -> {
                rotateCamera(dx, dy)
            }
            isSelected && Input.isMiddleDown -> {
                // move camera
                val fovYRadians = renderView.editorCamera.fovYDegrees.toRadians()
                val speed = tan(fovYRadians * 0.5f) * renderView.radius / height
                val camTransform = camera.transform!!
                val globalCamTransform = camTransform.globalTransform
                val offset = globalCamTransform.transformDirection(Vector3f(dx * speed, -dy * speed, 0f))
                renderView.orbitCenter.sub(offset)
                camera.invalidateBounds()
            }
            isSelected && Input.isLeftDown && mode != Mode.NOTHING && !Input.isShiftDown -> {

                val selection = EditorState.selection
                if (selection.isEmpty()) return
                val prefab = selection.firstInstanceOrNull(PrefabSaveable::class)?.root?.prefab ?: return
                if (!prefab.isWritable) {
                    LOGGER.warn("Prefab from '${prefab.sourceFile}' cannot be directly modified, inherit from it")
                    return
                }

                /**
                 * drag the selected object
                 **/
                // for that transform dx,dy into global space,
                // and then update the local space
                val speed = pixelsToWorldFactor
                val camTransform = camera.transform!!.globalTransform
                val offset = JomlPools.vec3f.create()
                offset.set(dx * speed, -dy * speed, 0.0)
                camTransform.transformDirection(offset)

                val gizmoMask = gizmoMask
                if (gizmoMask != 0) {
                    if (!gizmoMask.hasFlag(1)) offset.x = 0f
                    if (!gizmoMask.hasFlag(2)) offset.y = 0f
                    if (!gizmoMask.hasFlag(4)) offset.z = 0f
                }

                // rotate around the direction
                // we could use the average mouse position as center; this probably would be easier
                val instances = instancesToTransform
                val dir = renderView.cameraDirection
                val rotationAngle = if (mode == Mode.ROTATING) {
                    val pos = Vector3d()
                    val globalPosition = instances
                        .mapFirstNotNull { getTransformMatrix(it)?.getTranslation(pos) }

                    val speed1 = 5.0 * PI
                    speed1 * if (globalPosition != null) {
                        // project global position onto screen
                        val cameraPosition = globalPosition
                            .sub(renderView.cameraPosition)
                            .mulProject(renderView.cameraMatrix)

                        val mrx = (x - this.x) / this.width * 2.0 - 1.0
                        val mry = (y - this.y) / this.height * 2.0 - 1.0

                        val rx = mrx - cameraPosition.x
                        val ry = mry + cameraPosition.y // cam.y is flipped
                        (rx * dy - ry * dx) / height
                    } else {
                        // use center of screen
                        val rx = (x - (this.x + this.width * 0.5)) // [-w/2,+w/2]
                        val ry = (y - (this.y + this.height * 0.5)) // [-h/2,+h/2]
                        (rx * dy - ry * dx) / (height * height)
                    }
                } else 0.0

                // for correct transformation when parent and child are selected together
                val tmp = Vector3d()
                for (i in instances.indices) {
                    when (val inst = instances[i]) {
                        is Entity -> {
                            val transform = inst.transform
                            when (mode) {
                                Mode.TRANSLATING -> transformPosition(transform, camTransform, offset, i, tmp)
                                Mode.ROTATING -> transformRotation(transform, rotationAngle.toFloat(), dir, gizmoMask)
                                Mode.SCALING -> transformScale(transform, (dx - dy).toDouble(), offset)
                                else -> return
                            }
                            transform.teleportUpdate()
                            onChangeTransform(inst)
                        }
                        is DCMovable -> {
                            inst.move(this, camTransform, offset, dir, rotationAngle.toFloat(), dx, dy)
                        }
                    }
                }
                JomlPools.vec3f.sub(1)
            }
        }
    }

    val instancesToTransform: List<Any>
        get() {
            val targets3 = selectedEntities + selectedMovables
            // remove all targets of which there is a parent selected
            return targets3.filter { target ->
                val inHierarchy = (target as? Hierarchical<*>)?.listOfHierarchy?.toSet() ?: emptySet()
                targets3.none2 { it !== target && it in inHierarchy }
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

    fun transformPosition(transform: Transform, camTransform: Matrix4x3, offset: Vector3f, i: Int, tmp: Vector3d) {
        val global = transform.globalTransform
        if (i == 0) {
            val distance = camTransform.distance(global)
            if (distance > 0.0) {
                // correct
                offset.mul(distance, tmp)
                snappingSettings.snapPosition(tmp, snapPosRem)
                global.translateLocal(tmp)
                transform.invalidateLocal()
            }
        } else {
            global.translateLocal(tmp)
            transform.invalidateLocal()
        }
    }

    // todo re-test snapping
    private fun snapRotate(transform: Transform, rotationAngle: Float, dir: Vector3f, axis: Int) {
        transform.localRotation = transform.localRotation
            .toEulerAnglesDegrees()
            .apply {
                this[axis] += rotationAngle * sign(dir[axis]).toDegrees()
                snappingSettings.snapRotation(this, snapRotRem, 1 shl axis)
            }.toQuaternionDegrees()
    }

    fun transformRotation(transform: Transform, rotationAngle: Float, dir: Vector3f, gizmoMask: Int) {
        val snap = snappingSettings.snapR != 0f
        when (gizmoMask) {
            1 ->
                if (snap) snapRotate(transform, rotationAngle, dir, 0)
                else transform.globalRotation = transform.globalRotation.rotateLocalX(rotationAngle * sign(dir.x))
            2 ->
                if (snap) snapRotate(transform, rotationAngle, dir, 1)
                else transform.globalRotation = transform.globalRotation.rotateLocalY(rotationAngle * sign(dir.y))
            4 ->
                if (snap) snapRotate(transform, rotationAngle, dir, 2)
                else transform.globalRotation = transform.globalRotation.rotateLocalZ(rotationAngle * sign(dir.z))
            else -> {
                if (snap) {
                    val global = transform.globalRotation
                    val tmpQ = JomlPools.quat4f.create()
                    tmpQ.fromAxisAngleRad(dir.x, dir.y, dir.z, rotationAngle)
                    global.mul(tmpQ, tmpQ)
                    val tmpR = tmpQ.toEulerAnglesDegrees()
                    snappingSettings.snapRotation(tmpR, snapRotRem)
                    tmpR.toQuaternionDegrees(tmpQ)
                    transform.globalRotation = tmpQ
                    JomlPools.quat4f.sub(1)
                } else {
                    transform.globalRotation = Quaternionf()
                        .rotateAxis(rotationAngle, dir)
                        .mul(transform.globalRotation)
                }
            }
        }
    }

    fun transformScale(transform: Transform, delta: Double, offset: Vector3f) {
        if (gizmoMask == 0) {
            val scale = pow(2.0, delta / height).toFloat()
            transform.localScale = transform.localScale.mul(scale)
        } else {
            val base = 8f
            val sx = pow(base, offset.x)
            val sy = pow(base, offset.y)
            val sz = pow(base, offset.z)
            transform.localScale = transform.localScale.mul(sx, sy, sz)
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
            entity.invalidatePhysicsTransform()
        }
        invalidateInspector()
        PrefabInspector.currentInspector?.onChange(false)
    }

    fun addToParent(
        prefab: Prefab, root: PrefabSaveable, type: Char,
        position: Any?, rotation: Any?, scale: Any?,
        results: MutableCollection<PrefabSaveable>
    ) {
        val ci = PrefabInspector.currentInspector!!
        val path = ci.addNewChild(root, type, prefab)!!
        setTransform(ci, path, position, rotation, scale)
        val sample = Hierarchy.getInstanceAt(ci.root, path)
        if (sample != null) results.add(sample)
    }

    fun setTransform(
        ci: PrefabInspector, path: Path,
        position: Any?, rotation: Any?, scale: Any?
    ) {
        var hasValidTransform = true
        if (position != null) {
            hasValidTransform = false
            ci.prefab[path, "position"] = position
        }
        if (rotation != null) {
            hasValidTransform = false
            ci.prefab[path, "rotation"] = rotation
        }
        if (scale != null) {
            hasValidTransform = false
            ci.prefab[path, "scale"] = scale
        }
        if (!hasValidTransform) {
            // bug: on drop, position isn't correctly verified; only reloading fixes it
            ci.prefab.invalidateInstance() // todo fix: hack
            // todo sometimes dragging snaps wayyy too much... parent transform incorrect?
        }
    }

    private fun getDropTransform(
        root: Entity, sampleInstance: Entity?,
        dropPosition: Vector3d
    ): Triple<Vector3d, Quaternionf, Vector3f> {
        val newTransform = Matrix4x3()
            .translationRotateScale(dropPosition, dropRotation, dropScale)
        // we need to remove parent transform from this one
        root.transform.globalTransform
            .invert(Matrix4x3()).mul(newTransform, newTransform)
        if (sampleInstance != null) newTransform.mul(sampleInstance.transform.globalTransform)
        val position = newTransform.getTranslation(Vector3d())
        val rotation = newTransform.getUnnormalizedRotation(Quaternionf())
        val scale = newTransform.getScale(Vector3f())
        return Triple(position, rotation, scale)
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val hovered by lazy { // get hovered element
            renderView.resolveClick(x, y)
        }
        val hovEntity = hovered.first
        val hovComponent = hovered.second
        val results = ArrayList<PrefabSaveable>()
        val ci = PrefabInspector.currentInspector!!
        val dropPosition = findDropPosition(Vector3d())
        for (file in files) {
            onPasteFile(file, ci, hovEntity, hovComponent, dropPosition, results)
        }
        if (results.isNotEmpty()) {
            dragged = null
            requestFocus(true) // because we dropped sth here
        }
    }

    fun onPasteFile(
        file: FileReference, ci: PrefabInspector,
        hovEntity: Entity?, hovComponent: Component?,
        dropPosition: Vector3d, results: ArrayList<PrefabSaveable>
    ) {
        val prefab = PrefabCache[file].waitFor()?.prefab ?: return
        when (val sampleInstance = prefab.getSampleInstance()) {
            is Material -> {
                onPasteMaterial(file, hovComponent, sampleInstance)
            }
            is Entity -> {
                onPasteEntity(file, results, prefab, dropPosition, sampleInstance)
            }
            is DCDroppable -> sampleInstance.drop(
                this, prefab, hovEntity, hovComponent,
                dropPosition, dropRotation, dropScale, results
            )
            is Component -> {
                onPasteComponent(results, hovEntity, ci, prefab, dropPosition, sampleInstance)
            }
            else -> {
                // todo try to add it to all available, hovered and selected instances
                LOGGER.warn("Unknown type ${sampleInstance.className}")
            }
        }
        LOGGER.info("Pasted $file")
    }

    fun onPasteMaterial(
        file: FileReference, hovComponent: Component?,
        sampleInstance: Material,
    ) {
        when (hovComponent) {
            is DCPaintable -> hovComponent.paint(this, sampleInstance, file)
            is MeshComponentBase -> {
                val mesh = hovComponent.getMesh()
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
                                hovComponent.materials = createArrayList(numMaterials) {
                                    if (it == i) file
                                    else hovComponent.materials.getOrNull(it) ?: InvalidRef
                                }
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

    fun onPasteEntity(
        file: FileReference, results: ArrayList<PrefabSaveable>,
        prefab: Prefab, dropPosition: Vector3d, sampleInstance: Entity,
    ) {
        // add this to the scene
        // where? selected / root
        // done while dragging this, show preview
        // done place it where the preview was drawn
        val world = renderView.getWorld() as? Entity
        var root = EditorState.selection.firstInstanceOrNull(Entity::class) ?: world
        while (root != null && root != world && root.prefab?.findCAdd(root.prefabPath)
                .run { this == null || this.prefab != InvalidRef }
        ) {
            root = root.parent as? Entity ?: world
        }
        root = root ?: world
        if (root is Entity) {
            val (pos, rot, sca) =
                getDropTransform(root, sampleInstance, dropPosition)
            addToParent(prefab, root, 'e', pos, rot, sca, results)
        } else LOGGER.warn("Could not drop $file onto ${root?.className}")
    }

    fun onPasteComponent(
        results: ArrayList<PrefabSaveable>,
        hovEntity: Entity?, ci: PrefabInspector,
        prefab: Prefab, dropPosition: Vector3d, sampleInstance: Component,
    ) {
        val entity = hovEntity ?: renderView.getWorld() as? Entity
        if (entity != null) {
            if (sampleInstance is Renderable) {
                // if is Renderable & Component, add helper entity to define position
                val helperEntity = ci.addNewChild(entity, 'e', "Entity")
                val (pos, rot, sca) =
                    getDropTransform(entity, null, dropPosition)
                setTransform(ci, helperEntity, pos, rot, sca)
                val path = ci.addNewChild(helperEntity, 'c', prefab.clazzName, prefab.sourceFile)
                val sample = Hierarchy.getInstanceAt(ci.root, path)!!
                results.add(sample)
            } else {
                val path = ci.addNewChild(entity, 'c', prefab)!!
                val sample = Hierarchy.getInstanceAt(ci.root, path)
                if (sample != null) results.add(sample)
            }
        } else {
            LOGGER.warn("Entity is null")
            // this can happen when we paste a file
        }
    }

    val dropEuler = Vector3f()
    val dropRotation = Quaternionf()
    val dropScale = Vector3f(1f)

    fun findDropPosition(dst: Vector3d): Vector3d {
        // val prefab = PrefabCache[drop] ?: return null
        // val sample = prefab.getSampleInstance() as? Entity ?: return null
        // todo depending on mode, use other strategies to find zero-point on object
        // to do use mouse wheel to change height? maybe...
        // done depending on settings, we also can use snapping
        val camPos = renderView.mousePosition
        val camDir = renderView.mouseDirection
        var y0 = 0.0
        val snapY = snappingSettings.snapY
        if (snapY > 0.0) {
            y0 = snappingSettings.snap(camPos.y, snapY)
            if (camDir.y > 0.0 && y0 < camPos.y) {
                y0 += snapY
            }
            if (camDir.y < 0.0 && y0 > camPos.y) {
                y0 -= snapY
            }
        }
        var distance = (y0 - camPos.y) / camDir.y
        val world = renderView.getWorld()
        if (world is Entity) {
            val query = renderView.rayQuery()
            val hasHit = Raycast.raycast(world, query)
            if (hasHit) distance = query.result.distance
        }
        // to do camDirection will only be correct, if this was the last drawn instance
        dst.set(renderView.mouseDirection).mul(distance).add(renderView.mousePosition)
        snappingSettings.snapPosition(dst)
        if (camDir.y > 0.0 && dst.y < camPos.y) {
            dst.add(snappingSettings.snapY)
        }
        if (camDir.y < 0.0 && dst.y > camPos.y) {
            dst.sub(snappingSettings.snapY)
        }
        return dst
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT && Input.isShiftDown) {
            selectEntitiesInRectangle()
        } else super.onKeyUp(x, y, key)
    }

    private fun selectEntitiesInRectangle() {
        val window = window ?: return
        val root = renderView.getWorld() as? Entity ?: return
        val mouseBounds = AABBf()
            .union(window.mouseX, window.mouseY, 0f)
            .union(window.mouseDownX, window.mouseDownY, 1e10f)
            .translate(-(x + width * 0.5f), -(y + height * 0.5f), 0f)
            .scale(+2f / width, -2f / height, 1f)
        val tmp1 = Vector3d()
        val tmp2 = Vector3f()
        val bounds2d = AABBf()
        val cameraPos = renderView.cameraPosition
        val cameraMat = renderView.cameraMatrix
        EditorState.select(null) // start with empty selection
        root.forAllEntitiesInChildren(false) { entity ->
            components@ for (component in entity.components) {
                if (component is MeshComponentBase) {
                    bounds2d.clear()
                    val bounds3d = component.globalAABB
                    for (i in 0 until 8) {
                        tmp1.set(
                            if (i.hasFlag(1)) bounds3d.minX else bounds3d.maxX,
                            if (i.hasFlag(2)) bounds3d.minY else bounds3d.maxY,
                            if (i.hasFlag(4)) bounds3d.minZ else bounds3d.maxZ
                        ).sub(cameraPos)
                        cameraMat.transformProject(tmp2.set(tmp1))
                        if (tmp2.z > 0f) bounds2d.union(tmp2)
                    }
                    if (bounds2d.testAABB(mouseBounds)) {
                        EditorState.select(entity, true) // add entity to selection
                        break@components
                    }
                }
            }
        }
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        // get all selected items
        // make them into separate prefabs
        // pack them into temporary files (?)
        // then copy them, or their prefab text
        val cloneFiles = EditorState.selection
            .filterIsInstance<PrefabSaveable>()
            .map { it.clone().apply { unlinkPrefab() }.ref }
        LOGGER.info("Requested copy -> $cloneFiles")
        return cloneFiles
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

    override fun isKeyInput() = true
    override fun acceptsChar(char: Int) = true

    companion object {
        private val LOGGER = LogManager.getLogger(DraggingControls::class)
    }
}