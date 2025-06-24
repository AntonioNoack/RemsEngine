package me.anno.bullet.bodies

import com.bulletphysics.dynamics.vehicle.WheelInfo
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3

class VehicleWheel : Component(), OnDrawGUI {

    /*// raycast direction, e.g. down = -y axis
    @SerializedProperty
    var wheelDirection = org.joml.Vector3d(0.0, -1.0, 0.0)

    // turning direction, e.g. +/- x axis
    @SerializedProperty
    var wheelAxle = org.joml.Vector3d(-1.0, 0.0, 0.0)*/

    @NotSerializedProperty
    val rotation: Double
        get() = bulletInstance?.rotation ?: 0.0

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
    var suspensionDampingCompression = 0.83
        set(value) {
            field = value
            bulletInstance?.wheelDampingCompression = value
        }

    @SerializedProperty
    var suspensionDampingRelaxation = 0.88
        set(value) {
            field = value
            bulletInstance?.wheelDampingRelaxation = value
        }

    @SerializedProperty
    var maxSuspensionTravel = 5.0
        set(value) {
            field = value
            bulletInstance?.maxSuspensionTravel = value
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

    /**
     * How much friction each wheel has. 1.0 = full friction, 0.0 = sliding.
     * */
    @DebugProperty
    @NotSerializedProperty
    val skidInfo: Double get() = bulletInstance?.skidInfo ?: 1.0

    @DebugProperty
    val hasBulletInstance: Boolean get() = bulletInstance != null

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        // todo draw steering and power, brake and such for debugging
        LineShapes.drawCircle(entity, radius, 1, 2, 0.0)
    }

    @NotSerializedProperty
    var bulletInstance: WheelInfo? = null

    @NotSerializedProperty
    val lockedTransform = Matrix4x3()

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        val tmp = AABBd()
        val r = radius
        tmp.setMin(0.0, -r, -r)
        tmp.setMax(0.0, +r, +r)
        tmp.transformUnion(globalTransform, dstUnion)
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is VehicleWheel) return
        dst.radius = radius
        dst.brakeForce = brakeForce
        dst.engineForce = engineForce
        dst.steering = steering
        dst.rollInfluence = rollInfluence
        dst.suspensionDampingRelaxation = suspensionDampingRelaxation
        dst.suspensionStiffness = suspensionStiffness
        dst.suspensionRestLength = suspensionRestLength
        dst.suspensionDampingCompression = suspensionDampingCompression
        dst.maxSuspensionTravel = maxSuspensionTravel
        dst.steeringMultiplier = steeringMultiplier
        dst.engineForceMultiplier = engineForceMultiplier
        dst.brakeForceMultiplier = brakeForceMultiplier
        dst.frictionSlip = frictionSlip
    }
}