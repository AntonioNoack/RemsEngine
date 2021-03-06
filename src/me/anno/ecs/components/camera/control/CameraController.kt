package me.anno.ecs.components.camera.control

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.isShiftDown
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Floats.toRadians
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

    /**
     * euler yxz rotation where x=x,y=y,z=z
     * */
    var rotation = Vector3f()

    var movementSpeed = 1f
    var rotationSpeed = 1f

    @Type("Camera/PrefabSaveable")
    @NotSerializedProperty
    var camera: Camera? = null

    @Type("Entity/PrefabSaveable")
    @NotSerializedProperty
    var base: Entity? = null

    @Range(0.0, 100.0)
    var friction = 5f

    /** whether the motion will be rotated by the camera looking direction */
    var rotateAngleY = true
    var rotateAngleX = false
    var rotateAngleZ = false

    /** set to 0 to disable numpad as mouse; degrees per second */
    var numpadAsMouseSpeed = 90f
    var numpadWheelSpeed = 1f

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CameraController
        clone.base = getInClone(base, clone)
        clone.camera = getInClone(camera, clone)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(null, "base", base)
        writer.writeObject(null, "camera", camera)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "base" -> base = value as? Entity
            "camera" -> camera = value as? Camera
            else -> super.readObject(name, value)
        }
    }

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
        val s = movementSpeed
        if (isKeyDown('w') || isKeyDown(GLFW.GLFW_KEY_UP)) acceleration.z -= s
        if (isKeyDown('s') || isKeyDown(GLFW.GLFW_KEY_DOWN)) acceleration.z += s
        if (isKeyDown('a') || isKeyDown(GLFW.GLFW_KEY_LEFT)) acceleration.x -= s
        if (isKeyDown('d') || isKeyDown(GLFW.GLFW_KEY_RIGHT)) acceleration.x += s
        if (isShiftDown || isKeyDown('q')) acceleration.y -= s
        if (isKeyDown(' ') || isKeyDown('e')) acceleration.y += s
        if (numpadAsMouseSpeed != 0f || numpadWheelSpeed != 0f) {

            var dx = 0f
            var dy = 0f
            var dz = 0f

            if (isKeyDown("numpad4")) dx--
            if (isKeyDown("numpad6")) dx++
            if (isKeyDown("numpad8")) dy--
            if (isKeyDown("numpad2")) dy++
            if (isKeyDown("numpad7")) dz++
            if (isKeyDown("numpad1")) dz--

            if (dx != 0f || dy != 0f) {
                val dt = Engine.deltaTime * numpadAsMouseSpeed * rotationSpeed
                rotation.add((dy * dt).toRadians(), (dx * dt).toRadians(), 0f)
                clampRotation()
            }

            if (dz != 0f) {
                val dt = Engine.deltaTime * numpadWheelSpeed
                onMouseWheel(0f, 0f, 0f, dz * dt, false)
            }

        }
    }

    open fun clampAcceleration(acceleration: Vector3f) {
        val al = acceleration.length()
        if (al > movementSpeed) {
            acceleration.mul(movementSpeed / al)
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
        return if (!needsClickToRotate || (Input.isLeftDown && rotateLeft) || (Input.isMiddleDown && rotateMiddle) || (Input.isRightDown && rotateRight)) {
            rotation.add(-dy.toRadians() * rotationSpeed, -dx.toRadians() * rotationSpeed, 0f)
            clampRotation()
            // ... apply rotation to transform
            true
        } else false
    }

}