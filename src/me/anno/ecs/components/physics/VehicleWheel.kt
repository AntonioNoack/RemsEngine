package me.anno.ecs.components.physics

import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.dynamics.vehicle.WheelInfo
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.serialization.SerializedProperty
import javax.vecmath.Vector3d
import kotlin.math.abs

class VehicleWheel : Component() {

    // raycast direction
    @SerializedProperty
    var wheelDirection = org.joml.Vector3d(0.0, -1.0, 0.0)

    // turning direction
    @SerializedProperty
    var wheelAxle = org.joml.Vector3d(-1.0, 0.0, 0.0)

    @SerializedProperty
    var suspensionRestLength = 1.0

    @SerializedProperty
    var radius = 1.0
        set(value) {
            field = value
            bulletInstance?.wheelsRadius = value
        }

    @SerializedProperty
    var isFront = true
        set(value) {
            field = value
            bulletInstance?.bIsFrontWheel = value
        }

    @SerializedProperty
    var suspensionStiffness = 5.88

    @SerializedProperty
    var suspensionCompression = 0.83

    @SerializedProperty
    var suspensionDamping = 0.88

    @SerializedProperty
    var maxSuspensionTravelCm = 500.0

    @SerializedProperty
    var frictionSlip = 10.5

    var steering = 0.0
        set(value) {
            field = value
            bulletInstance?.steering = steering
        }

    var brakeForce = 0.0
        set(value) {
            field = value
            bulletInstance?.brake = value
        }

    var engineForce = 0.0
        set(value) {
            field = value
            bulletInstance?.engineForce = value
        }

    var rollInfluence = 0.1
        set(value) {
            field = value
            bulletInstance?.rollInfluence = value
        }

    override fun onDrawGUI() {
        super.onDrawGUI()

        // todo draw a circle for the radius, and wheel movement and such

    }

    var bulletInstance: WheelInfo? = null

    fun createBulletInstance(entity: Entity, vehicle: RaycastVehicle): WheelInfo {
        val transform = this.entity!!.fromLocalToOtherLocal(entity)
        val position = Vector3d(transform.m30(), transform.m31(), transform.m32())
        println(position)
        val wheelDirection1 = Vector3d(wheelDirection.x, wheelDirection.y, wheelDirection.z)
        val scale0 = transform.getScale(org.joml.Vector3d())
            .dot(wheelDirection.x, wheelDirection.y, wheelDirection.z)
        val scale = abs(scale0)
        val actualWheelRadius = radius * scale
        val wheelAxle1 = Vector3d(wheelAxle.x, wheelAxle.y, wheelAxle.z)
        val tuning = VehicleTuning()
        tuning.frictionSlip = tuning.frictionSlip
        tuning.suspensionDamping = suspensionDamping
        tuning.suspensionStiffness = suspensionStiffness
        tuning.suspensionCompression = suspensionCompression
        tuning.maxSuspensionTravelCm = maxSuspensionTravelCm
        val wheel = vehicle.addWheel(
            position, wheelDirection1, wheelAxle1,
            suspensionRestLength, actualWheelRadius,
            tuning, isFront
        )
        wheel.brake = brakeForce
        wheel.engineForce = engineForce
        wheel.steering = steering
        wheel.rollInfluence = rollInfluence
        return wheel
    }

    override val className: String = "VehicleWheel"

}