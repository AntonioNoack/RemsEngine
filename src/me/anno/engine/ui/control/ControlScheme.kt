package me.anno.engine.ui.control

import me.anno.Time.deltaTime
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.EngineBase
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.EditorState.control
import me.anno.engine.ui.EditorState.editMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Input.isKeyDown
import me.anno.input.Key
import me.anno.input.Touch
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

// todo touch controls

open class ControlScheme(val camera: Camera, val renderView: RenderView) :
    NineTilePanel(style) {

    constructor(renderView: RenderView) : this(renderView.editorCamera, renderView)

    override fun isOpaqueAt(x: Int, y: Int): Boolean = true

    val cameraNode = camera.entity!!

    open val settings = ControlSettings()

    val selectedEntities
        get() = EditorState.selection.mapNotNull {
            when (it) {
                is Component -> it.entity
                is Entity -> it
                else -> null
            }
        }

    val selectedTransforms get() = selectedEntities.map { it.transform }

    val isSelected get() = uiParent!!.isAnyChildInFocus

    val dirX = Vector3d()
    val dirY = Vector3d()
    val dirZ = Vector3d()
    private val rotQuad = Quaterniond()
    private val velocity = Vector3d()

    /**
     * add extra meshes like gizmos, and draggables into the scene
     * */
    open fun fill(pipeline: Pipeline) {}

    // transfer events
    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        invalidateDrawing()
        if (control?.onCharTyped(codepoint) == true) return
        if (editMode?.onEditTyped(codepoint) == true) return
        super.onCharTyped(x, y, codepoint)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        invalidateDrawing()
        if (control?.onKeyTyped(key) == true) return
        if (editMode?.onEditClick(key, false) == true) return
        super.onKeyTyped(x, y, key)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        invalidateDrawing()
        if (control?.onKeyDown(key) == true) return
        if (editMode?.onEditDown(key) == true) return
        super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        invalidateDrawing()
        if (control?.onKeyUp(key) == true) return
        if (editMode?.onEditUp(key) == true) return
        super.onKeyUp(x, y, key)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        invalidateDrawing()
        if (control?.onMouseClicked(button, long) == true) return
        if (editMode?.onEditClick(button, long) == true) return
        selectObjectAtCursor(x, y)
        testHits()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        invalidateDrawing()
        if (control?.onMouseMoved(x, y, dx, dy) == true) return
        if (editMode?.onEditMove(x, y, dx, dy) == true) return
        if (isSelected && Input.isRightDown) {
            rotateCamera(dx, dy)
        }
    }

    fun rotateCamera(dx: Float, dy: Float) {
        // right mouse key down -> move the camera
        var speed = -settings.turnSpeed * 500f / Maths.max(windowStack.height, height)
        if (camera.isPerspective) {
            speed *= atan(camera.fovY.toRadians())
        }
        rotateCamera(dy * speed, dx * speed, 0f)
    }

    // less than 90, so we always know forward when computing movement
    val limit = 90.0 - 0.001
    val rotationTarget = renderView.orbitRotation.getEulerAnglesYXZ(Vector3d())

    override fun onUpdate() {
        super.onUpdate()
        updateViewRotation()
        renderView.editorCamera.fovY = settings.fovY
    }

    open fun updateViewRotation() {
        val tmp = Quaterniond()
            .identity()
            .rotateY(rotationTarget.y.toRadians())
            .rotateX(rotationTarget.x.toRadians())
            .rotateZ(rotationTarget.z.toRadians())
        renderView.orbitRotation.slerp(tmp, Maths.dtTo01(deltaTime * 25.0))
        invalidateDrawing()
    }

    fun rotateCameraTo(vx: Float, vy: Float, vz: Float) =
        rotateCameraTo(vx.toDouble(), vy.toDouble(), vz.toDouble())

    fun rotateCameraTo(vx: Double, vy: Double, vz: Double) {
        rotationTarget.set(vx, vy, vz)
    }

    fun rotateCameraTo(v: Vector3f) = rotateCameraTo(v.x, v.y, v.z)

    open fun rotateCamera(vx: Float, vy: Float, vz: Float) {
        rotateCameraTo(clamp(rotationTarget.x + vx, -limit, +limit), rotationTarget.y + vy, rotationTarget.z + vz)
    }

    fun rotateCamera(v: Vector3f) {
        rotateCamera(v.x, v.y, v.z)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        invalidateDrawing()
        if (control?.onMouseWheel(x, y, dx, dy, byMouse) == true) return
        // not supported, will always be zooming
        // if (editMode?.onEditWheel(x, y, dx, dy) == true) return
        if (isSelected) {
            val factor = Maths.pow(0.5f, dy / 16f)
            zoom(factor)
            invalidateDrawing()
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        if (control?.onGotAction(x, y, dx, dy, action) == true) return true
        return super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    private fun selectObjectAtCursor(x: Float, y: Float) {
        // select the clicked thing in the scene
        val (e, c) = renderView.resolveClick(x, y)
        // show the entity in the property editor
        // but highlight the specific mesh
        ECSSceneTabs.refocus()
        EditorState.select(c ?: e, Input.isShiftDown)
    }

    open fun drawGizmos() {
    }

    fun invalidateInspector() {
        for (window in windowStack) {
            for (panel in window.panel.listOfVisible) {
                when (panel) {
                    is PropertyInspector -> {
                        panel.invalidate()
                    }
                }
            }
        }
    }

    private fun testHits() {
        val world = renderView.getWorld()
        val start = Vector3d(renderView.cameraPosition)
        val dir = renderView.mouseDirection
        val maxDistance = renderView.radius * 1e9
        val query = RayQuery(
            start, dir, maxDistance, -1, -1,
            false, emptySet()
        )
        val hit = when (world) {
            is Entity -> Raycast.raycastClosestHit(world, query)
            is CollidingComponent -> world.raycastClosestHit(query)
            is Mesh -> RaycastMesh.raycastGlobalMeshClosestHit(query, null, world)
            else -> return
        }
        if (hit) {
            val result = query.result
            val pos = result.positionWS
            val normal = result.geometryNormalWS.normalize(
                0.05 * result.positionWS.distance(renderView.cameraPosition)
            )
            // draw collision point
            debugPoints.add(DebugPoint(pos, -1))
            // draw collision normal
            debugLines.add(DebugLine(pos, normal.add(pos, Vector3d()), black or 0x00ff00))
        } else {
            // draw red point in front of the camera
            debugPoints.add(DebugPoint(Vector3d(dir).mul(20.0).add(start), black or 0xff0000))
        }
    }

    open fun checkMovement() {
        val view = renderView
        val dt = deltaTime
        val factor = clamp(20.0 * dt, 0.0, 1.0)
        val velocity = velocity.mul(1.0 - factor)
        val radius = view.radius
        var acceleration = factor * radius * settings.moveSpeed
        if (camera.isPerspective) {
            acceleration *= atan(camera.fovY.toRadians())
        }
        val shiftSpace = settings.enableShiftSpaceControls
        if (isSelected && !Input.isControlDown && (!Input.isShiftDown || shiftSpace) && !Input.isAltDown) {
            val down = isKeyDown(Key.KEY_Q) || (shiftSpace && Input.isShiftDown)
            val up = isKeyDown(Key.KEY_E) || (shiftSpace && isKeyDown(Key.KEY_SPACE))
            velocity.x += (isKeyDown(Key.KEY_D).toInt() - isKeyDown(Key.KEY_A).toInt()) * acceleration
            velocity.z += (isKeyDown(Key.KEY_S).toInt() - isKeyDown(Key.KEY_W).toInt()) * acceleration
            velocity.y += (up.toInt() - down.toInt()) * acceleration
        }
        if (velocity.lengthSquared() > 0.0001 * sq(radius)) {
            moveCamera(velocity.x * dt, velocity.y * dt, velocity.z * dt)
        }
        val position = view.orbitCenter
        if (!position.isFinite) {
            LOGGER.warn("Invalid position $position from $velocity * mat($dirX, $dirY, $dirZ)")
            position.set(0.0)
            Thread.sleep(100)
        }
    }

    open fun moveCamera(dx: Double, dy: Double, dz: Double) {
        val r = rotationTarget
        val fromDegrees = PI / 180
        val y = r.y * fromDegrees
        if (settings.changeYByWASD) {
            val x = r.x * fromDegrees
            val z = r.z * fromDegrees
            val m = JomlPools.mat3d.borrow()
            m.identity().rotateYXZ(y, x, z).transpose()
            dirX.set(1.0, 0.0, 0.0).mul(m)
            dirY.set(0.0, 1.0, 0.0).mul(m)
            dirZ.set(0.0, 0.0, 1.0).mul(m)
        } else {
            val c = cos(y)
            val s = sin(y)
            dirX.set(+c, 0.0, +s)
            dirZ.set(-s, 0.0, +c)
            dirY.set(0.0, 1.0, 0.0)
        }
        renderView.orbitCenter.add(
            dirX.dot(dx, dy, dz),
            dirY.dot(dx, dy, dz),
            dirZ.dot(dx, dy, dz)
        )
    }

    // to do call events before we draw the scene? that way we would not get the 1-frame delay
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // no background
        if (renderView.renderMode == RenderMode.RAY_TEST) {
            testHits()
        }
        parseTouchInput()
        makeBackgroundTransparent()
        super.onDraw(x0, y0, x1, y1)
        checkMovement()
    }

    fun parseTouchInput() {

        val first = Touch.touches.values.firstOrNull()
        if (first != null && contains(first.x0, first.y0)) {

            when (Touch.touches.size) {
                0, 1 -> {
                } // handled like a mouse
                2 -> {

                    val speed = -120f * EngineBase.shiftSlowdown / windowStack.height
                    // this gesture started on this view -> this is our gesture
                    // rotating is the hardest on a touchpad, because we need to click right
                    // -> rotation
                    val dx = Touch.sumDeltaX() * speed
                    val dy = Touch.sumDeltaY() * speed
                    rotateCamera(dy, dx, 0f)

                    // zoom in/out
                    val zoomFactor = Touch.getZoomFactor()
                    zoom(zoomFactor)// power 1 is too slow

                    println("Zooming by $zoomFactor, rotating $dx,$dy")

                    Touch.updateAll()
                }
                else -> {

                    // move the camera around
                    val speed = -3f * renderView.radius / windowStack.height

                    val dx = Touch.avgDeltaX() * speed
                    val dy = Touch.avgDeltaY() * speed
                    if (Input.isShiftDown) {
                        moveCamera(dx, -dy, 0.0)
                    } else {
                        moveCamera(dx, 0.0, dy)
                    }

                    // zoom in/out
                    val r = Touch.getZoomFactor()
                    zoom(r * r * sign(r))// power 1 is too slow

                    Touch.updateAll()
                }
            }
        }
    }

    fun zoom(factor: Float) {
        renderView.radius *= factor
        renderView.near *= factor
        renderView.far *= factor
        camera.fovOrthographic *= factor
    }

    override val className: String
        get() = "ControlScheme"

    companion object {
        private val LOGGER = LogManager.getLogger(ControlScheme::class)
    }
}