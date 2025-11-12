package me.anno.bullet.bodies

import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty

class Vehicle : DynamicBody() {

    @SerializedProperty
    var suspensionStiffness = 5.88f

    @SerializedProperty
    var suspensionCompression = 0.83f

    @SerializedProperty
    var suspensionDamping = 0.88f

    @SerializedProperty
    var maxSuspensionTravelCm = 500f

    // todo test this: this should be the threshold for drifting
    @SerializedProperty
    var frictionSlip = 10.5f

    @DebugProperty
    val wheelCount get() = wheels.size

    val wheels get() = getComponentsInChildren(VehicleWheel::class)

    @DebugProperty
    @Docs("Uniform getter/setter")
    @NotSerializedProperty
    var steering: Float
        get() {
            val wheels = wheels
            for (i in wheels.indices) {
                val wheel = wheels[i]
                if (wheel.steeringMultiplier != 0f) {
                    return wheel.steering / wheel.steeringMultiplier
                }
            }
            return 0f
        }
        set(value) {
            val wheels = wheels
            for (i in wheels.indices) {
                val wheel = wheels[i]
                wheel.steering = value * wheel.steeringMultiplier
            }
        }

    @DebugProperty
    @Docs("Uniform getter/setter")
    @NotSerializedProperty
    var engineForce: Float
        get() = wheels.firstOrNull()?.engineForce ?: 0f
        set(value) {
            val wheels = wheels
            for (i in wheels.indices) {
                wheels[i].engineForce = value
            }
        }

    @DebugProperty
    @Docs("Uniform getter/setter")
    @NotSerializedProperty
    var brakeForce: Float
        get() = wheels.firstOrNull()?.brakeForce ?: 0f
        set(value) {
            val wheels = wheels
            for (i in wheels.indices) {
                wheels[i].brakeForce = value
            }
        }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Vehicle) return
        dst.suspensionDamping = suspensionDamping
        dst.suspensionStiffness = suspensionStiffness
        dst.suspensionCompression = suspensionCompression
        dst.maxSuspensionTravelCm = maxSuspensionTravelCm
        dst.frictionSlip = frictionSlip
    }
}