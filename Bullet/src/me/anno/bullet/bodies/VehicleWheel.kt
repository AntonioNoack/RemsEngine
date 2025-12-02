package me.anno.bullet.bodies

import com.bulletphysics.dynamics.vehicle.WheelInstance
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.FillSpace
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3

class VehicleWheel : Component(), OnDrawGUI, FillSpace {

    /*// raycast direction, e.g. down = -y axis
   
    var wheelDirection = org.joml.Vector3d(0.0, -1.0, 0.0)

    // turning direction, e.g. +/- x axis
   
    var wheelAxle = org.joml.Vector3d(-1.0, 0.0, 0.0)*/

    @NotSerializedProperty
    var rotation: Double = 0.0

    var radius = 1f

    var suspensionRestLength = 1f

    /**
     * unit: force / meter
     * */
    var suspensionStiffness = 5.88f
    var suspensionDampingCompression = 0.83f
    var suspensionDampingRelaxation = 0.88f
    var maxSuspensionTravel = 5f

    var frictionSlip = 10.5f

    @Docs("When a wheel controller is used, it should multiply its steering by this before applying it")
    var steeringMultiplier = 1f
    var steering = 0f

    @Docs("When a wheel controller is used, it should multiply its engine force by this before applying it")
    var engineForceMultiplier = 1f
    var engineForce = 0f

    @Docs("When a wheel controller is used, it should multiply its brake force by this before applying it")
    var brakeForceMultiplier = 1f
    var brakeForce = 0f

    var rollInfluence = 0.1f

    /**
     * How much friction each wheel has. 1.0 = full friction, 0.0 = sliding.
     * */
    @DebugProperty
    @NotSerializedProperty
    var skidInfo: Float = 1f

    @DebugProperty
    val hasBulletInstance: Boolean get() = bulletInstance != null

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        // todo draw steering and power, brake and such for debugging
        LineShapes.drawCircle(entity, radius.toDouble(), 1, 2, 0.0)
    }

    @NotSerializedProperty
    var bulletInstance: WheelInstance? = null

    @NotSerializedProperty
    val lockedTransform = Matrix4x3()

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        val tmp = AABBd()
        val r = radius.toDouble()
        tmp.setMin(0.0, -r, -r)
        tmp.setMax(0.0, +r, +r)
        tmp.transformUnion(globalTransform, dstUnion)
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