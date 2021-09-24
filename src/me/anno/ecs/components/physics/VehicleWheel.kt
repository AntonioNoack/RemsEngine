package me.anno.ecs.components.physics

import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.dynamics.vehicle.WheelInfo
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes
import me.anno.engine.ui.render.RenderView
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
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
            bulletInstance?.suspensionRestLength1 = value
        }

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

    @SerializedProperty
    var brakeForce = 0.0
        set(value) {
            field = value
            bulletInstance?.brake = value
        }

    @SerializedProperty
    var engineForce = 0.0
        set(value) {
            field = value
            bulletInstance?.engineForce = value
        }

    @SerializedProperty
    var rollInfluence = 0.1
        set(value) {
            field = value
            bulletInstance?.rollInfluence = value
        }

    override fun onDrawGUI() {
        // todo draw steering and power, brake and such for debugging
        LineShapes.drawCircle(entity, radius, 1, 2, 0.0)
    }

    @NotSerializedProperty
    var bulletInstance: WheelInfo? = null

    fun createBulletInstance(entity: Entity, vehicle: RaycastVehicle): WheelInfo {
        val transform = this.entity!!.fromLocalToOtherLocal(entity)
        // +w
        val position = Vector3d(transform.m30(), transform.m31(), transform.m32())
        // val wheelDirection1 = Vector3d(wheelDirection.x, wheelDirection.y, wheelDirection.z)
        // raycast direction, e.g. down, so -y
        val wheelDirection1 = Vector3d(-transform.m10(), -transform.m11(), -transform.m12())
        val scale0 = transform.getScale(org.joml.Vector3d()).y
        // .dot(wheelDirection1.x, wheelDirection1.y, wheelDirection1.z)
        val scale = abs(scale0)
        val actualWheelRadius = radius * scale
        // wheel axis, e.g. x axis, so +x
        // val wheelAxle1 = Vector3d(wheelAxle.x, wheelAxle.y, wheelAxle.z)
        val wheelAxle1 = Vector3d(transform.m00(), transform.m01(), transform.m02())
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

    override fun clone(): VehicleWheel {
        val clone = VehicleWheel()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as VehicleWheel
        clone.radius = radius
        // clone.wheelAxle = wheelAxle
        // clone.wheelDirection = wheelDirection
        clone.brakeForce = brakeForce
        clone.engineForce = engineForce
        clone.isFront = isFront
        clone.rollInfluence = rollInfluence
        clone.suspensionDamping = suspensionDamping
        clone.suspensionStiffness = suspensionStiffness
        clone.suspensionRestLength = suspensionRestLength
        clone.suspensionCompression = suspensionCompression
        clone.maxSuspensionTravelCm = maxSuspensionTravelCm
        clone.steering = steering
        clone.frictionSlip = frictionSlip
    }

    override val className: String = "VehicleWheel"

}