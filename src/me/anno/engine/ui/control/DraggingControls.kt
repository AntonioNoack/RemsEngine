package me.anno.engine.ui.control

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.camDirection
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.ui.input.EnumInput
import me.anno.utils.maths.Maths.pow
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.distance
import org.apache.logging.log4j.LogManager
import org.joml.Math
import org.joml.Vector3d
import org.joml.Vector3f

// done controls
// done show the scene
// done drag stuff
// todo translate, rotate, scale with gizmos

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

    // todo start button
    // todo stop button

    init {
        // todo debugging view selection
        val topLeft = PanelListX(style)
        topLeft.add(EnumInput(
            "Draw Mode", "", view.renderMode.name,
            RenderView.RenderMode.values().map { NameDesc(it.name) }, style
        ).setChangeListener { _, index, _ ->
            view.renderMode = RenderView.RenderMode.values()[index]
        })
        topLeft.add(TextButton("Play", "Start the game", false, style)
            .addLeftClickListener {
                // todo change RenderView mode to play
                // todo also copy everything
                // todo also set this instance text to "Back"
                // todo like Unity, open a second tab(?)
            })
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
        drawGizmos()
    }

    override fun drawGizmos() {
        // todo also render this in the RenderView for click detection
        for (selected in EditorState.selection) {
            if (selected is Entity) {
                val scale = view.radius * 0.1
                val transform = selected.transform.globalTransform
                val pos = transform.getTranslation(JomlPools.vec3d.create())//.sub(RenderView.camPosition)
                when (mode) {
                    Mode.TRANSLATING -> {
                        Gizmos.drawTranslateGizmos(
                            RenderView.cameraMatrix,
                            pos,// mul world scale?
                            scale, -12
                        )
                    }
                    Mode.ROTATING -> {
                        Gizmos.drawRotateGizmos(
                            RenderView.cameraMatrix,
                            pos,// mul world scale?
                            scale, -12
                        )
                    }
                    Mode.SCALING -> {
                        Gizmos.drawScaleGizmos(
                            RenderView.cameraMatrix,
                            pos,// mul world scale?
                            scale, -12
                        )
                    }
                    else -> {
                    }
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
                view.updateEditorCameraTransform()
            }
            "Cam5" -> {// switch between orthographic and perspective
                // todo switch between ortho and perspective
                // camera.putValue(camera.orthographicness, 1f - camera.orthographicness[cameraTime], true)
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
        }
        return super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    fun rotateCameraTo(v: Vector3f) {
        view.rotation.set(v)
        view.updateEditorCameraTransform()
    }

    fun rotateCamera(v: Vector3f) {
        view.rotation.add(v)
        view.updateEditorCameraTransform()
    }

    fun turn(dx: Float, dy: Float) {
        // todo turn the camera
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (EditorState.control?.onMouseMoved(x, y, dx, dy) == true) return
        if (EditorState.editMode?.onEditMove(x, y, dx, dy) == true) return
        // super.onMouseMoved(x, y, dx, dy)
        if (isSelected && Input.isRightDown) {
            moveCamera(dx, dy)
        } else if (isSelected && Input.isLeftDown) {
            val targets = selectedEntities
            if (targets.isNotEmpty() && mode != Mode.NOTHING) {

                val prefab = targets.first().root.prefab!!
                if (!prefab.isWritable) {
                    LOGGER.warn("Prefab from '${prefab.source}' cannot be directly modified, inherit from it")
                    return
                }

                // drag the selected object
                // for that transform dx,dy into global space,
                // and then update the local space
                val fovYRadians = view.editorCamera.fovY
                val speed = Math.tan(fovYRadians * 0.5) / h // todo include fov in this calculation
                val camTransform = camera.transform!!.globalTransform
                val offset = camTransform.transformDirection(Vector3d(dx * speed, -dy * speed, 0.0))
                val sorted = targets.sortedBy { it.depthInHierarchy }
                val mode = mode

                // rotate around the direction
                // we could use the average mouse position as center; this probably would be easier
                val dir = camDirection
                val rx = (x - (this.x + this.w * 0.5)) / h
                val ry = (y - (this.y + this.h * 0.5)) / h // [-.5,+.5]
                val rotationAngle = rx * dy - ry * dx

                val tmpQ = JomlPools.quat4d.borrow()

                for (entity in sorted) {// for correct transformation when parent and child are selected together
                    val transform = entity.transform
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
                    transform.validate()
                    onChangeTransform(entity)
                }
            }
        }
    }

    private fun onChangeTransform(entity: Entity) {
        // save changes to file
        val root = entity.getRoot(Entity::class)
        val prefab = root.prefab!!
        val path = entity.prefabPath!!
        val transform = entity.transform
        prefab.set(path, "position", transform.localPosition)
        prefab.set(path, "rotation", transform.localRotation)
        prefab.set(path, "scale", transform.localScale)
        // entity.invalidateAABBsCompletely()
        // entity.invalidateChildTransforms()
        invalidateInspector()
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val hovered = lazy { // get hovered element
            val (e, c) = view.resolveClick(x, y)
            c
        }
        for (file in files) {
            // todo load as prefab (?)
            // todo when a material, assign it to the hovered mesh-component
            val prefab = loadPrefab(file) ?: continue
            when (prefab.clazzName) {
                "Material" -> {
                    val mesh = hovered.value as? MeshComponent

                    // todo set this material in the prefab
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
                "Entity" -> {
                    // add this to the scene
                    // where? selected / root
                    val root = library.selection.filterIsInstance<PrefabSaveable>().firstOrNull() ?: library.world
                    if (root is Entity) PrefabInspector.currentInspector!!.addEntityChild(root, prefab)
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

    override val className: String = "SceneView"

    companion object {
        private val LOGGER = LogManager.getLogger(DraggingControls::class)
    }

}