package me.anno.ecs.components.physics

import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.dynamics.vehicle.WheelInfo
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.utils.types.Matrices.getScaleLength
import javax.vecmath.Vector3d
import kotlin.math.abs

class VehicleWheel : Component() {

    /*// raycast direction, e.g. down = -y axis
    @SerializedProperty
    var wheelDirection = org.joml.Vector3d(0.0, -1.0, 0.0)

    // turning direction, e.g. +/- x axis
    @SerializedProperty
    var wheelAxle = org.joml.Vector3d(-1.0, 0.0, 0.0)*/

    @SerializedProperty
    var suspensionRestLength = 1.0
        set(value) {
            field = value
            bulletInstance?.suspensionRestLength = value
        }

    @SerializedProperty
    var radius = 1.0
        set(value) {
            field = value
            bulletInstance?.wheelRadius = value
        }

    @SerializedProperty
    var suspensionStiffness = 5.88
        set(value) {
            field = value
            bulletInstance?.suspensionStiffness = value
        }

    @SerializedProperty
    var suspensionCompression = 0.83
        set(value) {
            field = value
            // todo setter in bullet instance does not exist
            // bulletInstance?.sus
        }

    @SerializedProperty
    var suspensionDamping = 0.88
        set(value) {
            field = value
            // todo setter in bullet instance does not exist
            // bulletInstance?.sus
        }

    @SerializedProperty
    var maxSuspensionTravelCm = 500.0
        set(value) {
            field = value
            bulletInstance?.maxSuspensionTravelCm = value
        }

    @SerializedProperty
    var frictionSlip = 10.5
        set(value) {
            field = value
            bulletInstance?.frictionSlip = value
        }

    @SerializedProperty
    var steering = 0.0
        set(value) {
            field = value
            bulletInstance?.steering = value
        }

    @Docs("When a wheel controller is used, it should multiply its steering by this before applying it")
    @SerializedProperty
    var steeringMultiplier = 1.0

    @SerializedProperty
    var engineForce = 0.0
        set(value) {
            field = value
            bulletInstance?.engineForce = value
        }

    @Docs("When a wheel controller is used, it should multiply its engine force by this before applying it")
    @SerializedProperty
    var engineForceMultiplier = 1.0

    @SerializedProperty
    var brakeForce = 0.0
        set(value) {
            field = value
            bulletInstance?.brake = value
        }

    @Docs("When a wheel controller is used, it should multiply its brake force by this before applying it")
    @SerializedProperty
    var brakeForceMultiplier = 1.0

    @SerializedProperty
    var rollInfluence = 0.1
        set(value) {
            field = value
            bulletInstance?.rollInfluence = value
        }

    @DebugProperty
    @NotSerializedProperty
    val skidInfo: Double get() = bulletInstance?.skidInfo ?: 0.0

    override fun onDrawGUI(all: Boolean) {
        // todo draw steering and power, brake and such for debugging
        LineShapes.drawCircle(entity, radius, 1, 2, 0.0)
    }

    @NotSerializedProperty
    var bulletInstance: WheelInfo? = null

    fun createBulletInstance(entity: Entity, vehicle: RaycastVehicle): WheelInfo {
        val transform = this.entity!!.fromLocalToOtherLocal(entity)
        // +w
        val position = Vector3d(transform.m30, transform.m31, transform.m32)
        // raycast direction, e.g. down, so -y
        val wheelDirection = Vector3d(-transform.m10, -transform.m11, -transform.m12)
        val scale = abs(transform.getScaleLength() / SQRT3)
        val actualWheelRadius = radius * scale
        // wheel axis, e.g. x axis, so +x
        val wheelAxle = Vector3d(-transform.m00, -transform.m01, -transform.m02)
        val tuning = VehicleTuning()
        tuning.frictionSlip = tuning.frictionSlip
        tuning.suspensionDamping = suspensionDamping
        tuning.suspensionStiffness = suspensionStiffness
        tuning.suspensionCompression = suspensionCompression
        tuning.maxSuspensionTravelCm = maxSuspensionTravelCm
        val wheel = vehicle.addWheel(
            position, wheelDirection, wheelAxle,
            suspensionRestLength, actualWheelRadius,
            tuning, false // isFrontWheel does nothing
        )
        wheel.brake = brakeForce
        wheel.engineForce = engineForce
        wheel.steering = steering
        wheel.rollInfluence = rollInfluence
        return wheel
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as VehicleWheel
        dst.radius = radius
        dst.brakeForce = brakeForce
        dst.engineForce = engineForce
        dst.steering = steering
        dst.rollInfluence = rollInfluence
        dst.suspensionDamping = suspensionDamping
        dst.suspensionStiffness = suspensionStiffness
        dst.suspensionRestLength = suspensionRestLength
        dst.suspensionCompression = suspensionCompression
        dst.maxSuspensionTravelCm = maxSuspensionTravelCm
        dst.steeringMultiplier = steeringMultiplier
        dst.engineForceMultiplier = engineForceMultiplier
        dst.brakeForceMultiplier = brakeForceMultiplier
        dst.frictionSlip = frictionSlip
    }

    override val className: String get() = "VehicleWheel"

}