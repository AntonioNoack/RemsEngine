package me.anno.engine.ui.control

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.camDirection
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.utils.Maths.pow
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.distance
import org.joml.Math
import org.joml.Vector3d
import org.joml.Vector3f

// todo mode to place it on top of things using mesh bounds

// todo draw the gizmos

// todo shift to activate g/s/r-number control modes for exact scaling? mmh..

class DraggingControls(view: RenderView) : ControlScheme(view) {

    var mode = Mode.ROTATING

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

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "SetMode(MOVE)" -> {
                mode = Mode.TRANSLATING
                invalidateDrawing()
            }
            "SetMode(ROTATE)" -> {
                mode = Mode.ROTATING
                invalidateDrawing()
            }
            "SetMode(SCALE)" -> {
                mode = Mode.SCALING
                invalidateDrawing()
            }
            "Cam0", "ResetCamera" -> {
                // reset the camera
                view.rotation.set(0.0)
                view.position.set(0.0)
                view.radius = 50.0
                view.updateTransform()
                invalidateDrawing()
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
        view.updateTransform()
    }

    fun rotateCamera(v: Vector3f) {
        view.rotation.add(v)
        view.updateTransform()
    }

    fun turn(dx: Float, dy: Float) {
        // todo turn the camera
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (Input.isLeftDown && isSelected) {
            val targets = selectedEntities
            if (targets.isNotEmpty()) {
                // drag the selected object
                // for that transform dx,dy into global space,
                // and then update the local space
                val fovYRadians = view.editorCamera.fov
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

                for (target in sorted) {// for correct transformation when parent and child are selected together
                    val transform = target.transform
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
                        else -> {
                            // not yet implemented
                        }
                    }
                }
                for (entity in sorted) {
                    val transform = entity.transform
                    transform.calculateLocalTransform(entity.parentEntity?.transform)
                    transform.teleportUpdate()
                    onChangeTransform(entity)
                }
            }
        }
    }

    private fun onChangeTransform(entity: Entity) {
        // save changes to file
        val i = ECSSceneTabs.currentTab!!.inspector
        val path = entity.pathInRoot2(i.root, false)
        val transform = entity.transform
        i.change(path, "position", transform.localPosition)
        i.change(path, "rotation", transform.localRotation)
        i.change(path, "scale", transform.localScale)
        entity.invalidateAABBsCompletely()
        entity.invalidateChildTransforms()
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val hovered = lazy { // get hovered element
            val (e, c) = view.resolveClick(x, y)
            c
        }
        for (file in files) {
            // todo load as prefab (?)
            // todo when a material, assign it to the hovered mesh-component
            val prefab = Prefab.loadPrefab(file) ?: continue
            when (prefab.clazzName) {
                "Material" -> {
                    val mesh = hovered.value as? Mesh
                    if (mesh != null) {
                        val instance = prefab.createInstance() as Material
                        mesh.materials = listOf(instance)
                        // add this change
                        val inspector = PrefabInspector.currentInspector
                        if (inspector != null) {
                            val path = mesh.pathInRoot2(inspector.root, true)
                            // inject the mesh into the path
                            path.setLast(mesh.name, 0, 'm')
                            inspector.change(path, "materials", mesh.materials)
                        }
                    }
                }
                "Entity" -> {
                    // add this to the scene
                    // where? selected / root
                    val root = library.selection.filterIsInstance<Entity>().firstOrNull() ?: library.world
                    PrefabInspector.currentInspector!!.addEntityChild(root, prefab)
                }
                // todo general listener in the components, which listens for drag events? they could be useful for custom stuff...
                else -> {
                    // mmmh...
                    // todo add that component?
                }
            }
            println("pasted $file")
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        super.onPaste(x, y, data, type)
        println("pasted $data/$type")
    }

    override val className: String = "SceneView"

}