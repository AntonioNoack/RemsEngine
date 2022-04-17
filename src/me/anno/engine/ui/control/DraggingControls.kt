package me.anno.engine.ui.control

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.modifiers.DistanceMapper
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabCache.getPrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.camDirection
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.DepthMode
import me.anno.gpu.OpenGL
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.input.Input
import me.anno.input.Touch
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.pow
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.ui.input.EnumInput
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Matrices.distance
import me.anno.utils.types.Matrices.getScaleLength
import me.anno.utils.types.Matrices.set2
import org.apache.logging.log4j.LogManager
import org.joml.Math
import org.joml.Vector3d
import org.joml.Vector3f

// done controls
// done show the scene
// done drag stuff
// todo translate, rotate, scale with gizmos
// todo gizmos & movement for properties with @PositionAnnotation

// todo gui depth doesn't match scene depth.. why?

// todo advanced snapping
// todo mode to place it on top of things using mesh bounds
// todo xyz keys to rotate 90° on that axis
// todo shift for dynamic angles
// automatically attach it to that object, that is being targeted? mmh.. no, use the selection for that

// todo draw the gizmos

// todo shift to activate g/s/r-number control modes for exact scaling? mmh..

class DraggingControls(view: RenderView) : ControlScheme(view) {

    var mode = Mode.TRANSLATING

    init {
        // todo debugging view selection
        val topLeft = PanelListX(style)
        topLeft.add(EnumInput(
            "Draw Mode", "", view.renderMode.name,
            RenderMode.values().map { NameDesc(it.name) }, style
        ).setChangeListener { _, index, _ ->
            view.renderMode = RenderMode.values()[index]
        })
        topLeft.add(TextButton("Play", "Start the game", false, style)
            .addLeftClickListener {
                ECSSceneTabs.currentTab?.play()
                // todo also set this instance text to "Back"
            })
        if (view.playMode == PlayMode.PLAY_TESTING) {
            topLeft.add(TextButton("Pause", "", false, style)
                .addLeftClickListener {
                    // todo pause/unpause
                    // todo change text accordingly,
                })
            topLeft.add(TextButton("Restart", "", false, style)
                .addLeftClickListener {
                    view.getWorld()?.prefab?.invalidateInstance()
                })
        }
        add(topLeft)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // show the mode
        drawSimpleTextCharByChar(
            x, y, 2, when (mode) {
                Mode.TRANSLATING -> "T"
                Mode.ROTATING -> "R"
                Mode.SCALING -> "S"
                else -> "X"
            }
        )
    }

    override fun drawGizmos() {
        OpenGL.depthMode.use(DepthMode.ALWAYS) {
            OpenGL.depthMask.use(RenderView.currentInstance?.renderMode == RenderMode.DEPTH) {
                drawGizmos2()
            }
        }
    }

    private fun drawGizmos2(){
        for (selected0 in EditorState.selection) {
            var selected: PrefabSaveable? = selected0 as? PrefabSaveable
            while (selected != null && selected !is Entity) {
                selected = selected.parent
            }
            // todo gizmos for sdf components
            if (selected is Entity) {
                val scale = view.radius * 0.1
                val transform = selected.transform.globalTransform
                val pos = transform.getTranslation(JomlPools.vec3d.create())
                val cam = RenderView.cameraMatrix
                when (mode) {
                    Mode.TRANSLATING -> Gizmos.drawTranslateGizmos(cam, pos, scale, -12)
                    Mode.ROTATING -> Gizmos.drawRotateGizmos(cam, pos, scale, -12)
                    Mode.SCALING -> Gizmos.drawScaleGizmos(cam, pos, scale, -12)
                    Mode.NOTHING -> {}
                }
                JomlPools.vec3d.sub(1)
            }
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "SetMode(MOVE)" -> mode = Mode.TRANSLATING
            "SetMode(ROTATE)" -> mode = Mode.ROTATING
            "SetMode(SCALE)" -> mode = Mode.SCALING
            "Cam0", "ResetCamera" -> {
                // reset the camera
                view.rotation.set(0.0)
                view.position.set(0.0)
                view.radius = 50.0
                camera.fovOrthographic = 50f
                camera.fovY = 90f
                camera.isPerspective = true
                view.updateEditorCameraTransform()
            }
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

    fun turn(dx: Float, dy: Float) {
        // todo turn the camera
    }

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
                val fovYRadians = view.editorCamera.fovY
                val speed = Math.tan(fovYRadians * 0.5) * view.radius / h
                val camTransform = camera.transform!!
                val globalCamTransform = camTransform.globalTransform
                val offset = globalCamTransform.transformDirection(Vector3d(dx * speed, -dy * speed, 0.0))
                view.position.sub(offset)
            }
            isSelected && Input.isLeftDown && mode != Mode.NOTHING -> {

                val selection = library.selection
                if (selection.isEmpty()) return
                val prefab = selection.firstInstanceOrNull<PrefabSaveable>()?.root?.prefab ?: return
                if (!prefab.isWritable) {
                    LOGGER.warn("Prefab from '${prefab.source}' cannot be directly modified, inherit from it")
                    return
                }

                // drag the selected object
                // for that transform dx,dy into global space,
                // and then update the local space
                val fovYRadians = view.editorCamera.fovY
                val speed = Math.tan(fovYRadians * 0.5) / h
                val camTransform = camera.transform!!.globalTransform
                val offset = JomlPools.vec3d.create()
                offset.set(dx * speed, -dy * speed, 0.0)
                camTransform.transformDirection(offset)

                // rotate around the direction
                // we could use the average mouse position as center; this probably would be easier
                val dir = camDirection
                val rx = (x - (this.x + this.w * 0.5)) / h
                val ry = (y - (this.y + this.h * 0.5)) / h // [-.5,+.5]
                val rotationAngle = rx * dy - ry * dx

                val targets = selectedEntities
                if (targets.isNotEmpty()) {
                    val sorted = targets.sortedBy { it.depthInHierarchy }
                    for (inst in sorted) {// for correct transformation when parent and child are selected together
                        val transform = inst.transform
                        val global = transform.globalTransform
                        when (mode) {
                            Mode.TRANSLATING -> {
                                val distance = camTransform.distance(global)
                                if (distance > 0.0) {
                                    global.translateLocal(// correct
                                        offset.x * distance,
                                        offset.y * distance,
                                        offset.z * distance
                                    )
                                }
                            }
                            Mode.ROTATING -> {
                                val tmpQ = JomlPools.quat4d.borrow()
                                tmpQ.identity()
                                    .fromAxisAngleDeg(dir.x, dir.y, dir.z, rotationAngle)
                                global.rotate(tmpQ)// correct

                            }
                            Mode.SCALING -> {
                                val scale = pow(2.0, (dx - dy).toDouble() / h)
                                global.scale(scale, scale, scale) // correct
                            }
                            Mode.NOTHING -> {
                            }
                        }
                    }
                    for (entity in sorted) {
                        val transform = entity.transform
                        transform.invalidateLocal()
                        transform.teleportUpdate()
                        onChangeTransform(entity)
                    }
                    JomlPools.vec3d.sub(1)
                }
                val targets2 = selectedSDFs
                if (targets2.isNotEmpty()) {
                    val sorted = targets2.sortedBy { it.depthInHierarchy }
                        .map { Pair(it, it.computeGlobalTransform(JomlPools.mat4x3f.create().identity())) }
                    for ((_, global) in sorted) {
                        when (mode) {
                            Mode.TRANSLATING -> {
                                val distance = camTransform.distance(global)
                                if (distance > 0.0) {
                                    global.translateLocal(// correct
                                        (offset.x * distance).toFloat(),
                                        (offset.y * distance).toFloat(),
                                        (offset.z * distance).toFloat()
                                    )
                                }
                            }
                            Mode.ROTATING -> {
                                val tmpQ = JomlPools.quat4f.borrow()
                                tmpQ.identity()
                                    .fromAxisAngleDeg(
                                        dir.x.toFloat(),
                                        dir.y.toFloat(),
                                        dir.z.toFloat(),
                                        rotationAngle.toFloat()
                                    )
                                global.rotate(tmpQ)// correct
                            }
                            Mode.SCALING -> {
                                val scale = pow(2f, (dx - dy) / h)
                                global.scale(scale, scale, scale) // correct
                            }
                            Mode.NOTHING -> {
                            }
                        }
                    }
                    // just like for Entities
                    for ((inst, globalTransform) in sorted) {
                        // = Transform.invalidateLocal() + update()
                        val localTransform = JomlPools.mat4x3f.create()
                        val parentGlobalTransform = when (val parent = inst.parent) {
                            is Entity -> JomlPools.mat4x3f.create().set2(parent.transform.globalTransform)
                            is SDFComponent -> parent.computeGlobalTransform(JomlPools.mat4x3f.create())
                            else -> null
                        }
                        if (parentGlobalTransform == null) {
                            localTransform.set(globalTransform)
                        } else {
                            localTransform.set(parentGlobalTransform)
                                .invert()
                                .mul(globalTransform)
                        }
                        // we have no better / other choice
                        if (!localTransform.isFinite) {
                            localTransform.identity()
                        }
                        localTransform.getTranslation(inst.position)
                        localTransform.getUnnormalizedRotation(inst.rotation)
                        inst.scale = localTransform.getScaleLength() / SQRT3.toFloat()
                        // trigger recompilation, if needed
                        inst.position = inst.position
                        inst.rotation = inst.rotation
                        // return matrix to pool
                        if (parentGlobalTransform != null) JomlPools.mat4x3f.sub(1)
                        onChangeTransform(inst)
                    }
                    JomlPools.mat4x3f.sub(sorted.size)
                }
            }
        }
    }

    val selectedSDFs
        get() = library.selection.mapNotNull {
            when (it) {
                is SDFComponent -> it
                is PositionMapper -> it.parent as? SDFComponent
                is DistanceMapper -> it.parent as? SDFComponent
                else -> null
            }
        }

    private fun onChangeTransform(entity: Entity) {
        // save changes to file
        val root = entity.getRoot(Entity::class)
        val prefab = root.prefab
        val path = entity.prefabPath
        if (prefab != null && path != null) {
            val transform = entity.transform
            prefab.setUnsafe(path, "position", transform.localPosition)
            prefab.setUnsafe(path, "rotation", transform.localRotation)
            prefab.setUnsafe(path, "scale", transform.localScale)
        }
        // entity.invalidateAABBsCompletely()
        entity.invalidateChildTransforms()
        invalidateInspector()
    }

    private fun onChangeTransform(entity: SDFComponent) {
        // save changes to file
        val root = entity.root
        val prefab = root.prefab
        val path = entity.prefabPath
        if (prefab != null && path != null) {
            prefab.set(path, "position", entity.position)
            prefab.set(path, "rotation", entity.rotation)
            prefab.set(path, "scale", entity.scale)
        }
        // entity.invalidateAABBsCompletely()
        // entity.invalidateChildTransforms()
        invalidateInspector()
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val hovered by lazy { // get hovered element
            view.resolveClick(x, y)
        }
        val hovEntity = hovered.first
        val hovComponent = hovered.second
        for (file in files) {
            // todo load as prefab (?)
            // todo when a material, assign it to the hovered mesh-component
            val prefab = getPrefab(file) ?: continue
            when (prefab.getSampleInstance()) {
                is Material -> {
                    val meshComponent = hovComponent as? MeshComponent
                    if (meshComponent != null) {
                        val mesh = meshComponent.getMesh()
                        val numMaterials = mesh?.numMaterials ?: 1
                        if (numMaterials < 2 || true) {
                            // assign material
                            meshComponent.materials = listOf(file)
                            meshComponent.prefab?.set(meshComponent, "materials", meshComponent.materials)
                        } else {
                            // todo ask for slot to place material
                            // todo what if there are multiple materials being dragged? :)
                            // todo set this material in the prefab
                        }
                    }
                    // todo if the prefab is not writable, create a prefab for that mesh, and replace the mesh...
                    /*if (mesh != null) {
                        mesh.materials = listOf(file)
                        // add this change
                        val inspector = PrefabInspector.currentInspector
                        if (inspector != null) {
                            val path = mesh.pathInRoot2(inspector.root, true)
                            // inject the mesh into the path
                            path.setLast(mesh.name, 0, 'm')
                            inspector.change(path, "materials", mesh.materials)
                        }
                    }*/
                }
                is Entity -> {
                    // add this to the scene
                    // where? selected / root
                    // todo while dragging this, show preview
                    // todo place it where the preview was drawn
                    val root = library.selection.firstInstanceOrNull<PrefabSaveable>() ?: view.getWorld()
                    if (root is Entity) PrefabInspector.currentInspector!!.addEntityChild(root, prefab)
                }
                /*is SDFComponent -> {
                    // todo add this...
                }*/
                is Component -> {
                    if (hovEntity != null) {
                        PrefabInspector.currentInspector!!.addEntityChild(hovEntity, prefab)
                    }
                }
                // todo general listener in the components, which listens for drag events? they could be useful for custom stuff...
                else -> {
                    // mmmh...
                    // todo add that component?
                }
            }
            LOGGER.info("pasted $file")
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        super.onPaste(x, y, data, type)
        LOGGER.info("pasted $data/$type")
    }

    override fun onDeleteKey(x: Float, y: Float) {
        for (child in EditorState.fineSelection) {
            if (child is PrefabSaveable) {
                val parent = child.parent
                if (parent != null) {
                    Hierarchy.removePathFromPrefab(parent.root.prefab!!, child)
                }
            }
        }
    }

    override val className: String = "SceneView"

    companion object {
        private val LOGGER = LogManager.getLogger(DraggingControls::class)
    }

}