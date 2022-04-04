package me.anno.ecs.components.camera.control

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Vectors.addSmoothly
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW

abstract class CameraController : Component(), ControlReceiver {

    var needsClickToRotate = false
    var rotateLeft = false
    var rotateMiddle = false
    var rotateRight = false

    val acceleration = Vector3f()
    val velocity = Vector3f()
    val position = Vector3f()
    var rotation = Vector3f()

    val camera: Camera? = null
    var base: Entity? = null

    @Range(0.0, 100.0)
    var friction = 5f

    /** whether the motion will be rotated by the camera looking direction */
    var rotateAngleY = true
    var rotateAngleX = false
    var rotateAngleZ = false

    open fun clampRotation() {}

    /**
     * can prevent velocities from being applied, e.g. when running against a wall (like in Digital Campus)
     * */
    open fun clampVelocity() {

    }

    /**
     * define the acceleration in [-1 .. +1] range. Larger values will be clamped.
     * */
    open fun collectInputs(acceleration: Vector3f) {
        // todo support as much as possible, best with some kind of keymapping...
        if (Input.isKeyDown('w')) acceleration.z--
        if (Input.isKeyDown(GLFW.GLFW_KEY_UP)) acceleration.z--
        if (Input.isKeyDown('s')) acceleration.z++
        if (Input.isKeyDown(GLFW.GLFW_KEY_DOWN)) acceleration.z++
        if (Input.isKeyDown('a')) acceleration.x--
        if (Input.isKeyDown(GLFW.GLFW_KEY_LEFT)) acceleration.x--
        if (Input.isKeyDown('d')) acceleration.x++
        if (Input.isKeyDown(GLFW.GLFW_KEY_RIGHT)) acceleration.x++
        if (Input.isShiftDown) acceleration.y--
        if (Input.isKeyDown('q')) acceleration.y--
        if (Input.isKeyDown(' ')) acceleration.y++
        if (Input.isKeyDown('e')) acceleration.y++
    }

    open fun clampAcceleration(acceleration: Vector3f) {
        val al = acceleration.length()
        if (al > 1f) {
            acceleration.div(al)
        }
    }

    override fun onUpdate(): Int {

        val camera = camera
        val camEntity = camera?.entity
        val base = base

        lastWarning = when {
            camera == null -> "Camera Missing"
            camEntity == null -> "Camera Missing Entity"
            base == null -> "Base Missing"
            else -> {

                lastWarning = null

                val dt = clamp(Engine.deltaTime * friction)

                acceleration.set(0f)

                collectInputs(acceleration)

                clampAcceleration(acceleration)

                velocity.addSmoothly(acceleration, dt)

                clampVelocity()

                position.addSmoothly(velocity, dt)

                if (rotateAngleY) position.rotateY(rotation.y)
                if (rotateAngleX) position.rotateX(rotation.x)
                if (rotateAngleZ) position.rotateZ(rotation.z)

                computeTransform(base.transform, camEntity.transform, camera)

                return 1
            }
        }
        return 5
    }

    abstract fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera)

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        return if (needsClickToRotate || (Input.isLeftDown && rotateLeft) || (Input.isMiddleDown && rotateMiddle) || (Input.isRightDown && rotateRight)) {
            rotation.add(dy, 0f, dx)
            clampRotation()
            // ... apply rotation to transform
            true
        } else false
    }

}