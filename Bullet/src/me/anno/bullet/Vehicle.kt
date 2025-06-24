package me.anno.bullet

import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty

class Vehicle : DynamicBody() {

    @SerializedProperty
    var suspensionStiffness = 5.88

    @SerializedProperty
    var suspensionCompression = 0.83

    @SerializedProperty
    var suspensionDamping = 0.88

    @SerializedProperty
    var maxSuspensionTravelCm = 500.0

    // todo test this: this should be the threshold for drifting
    @SerializedProperty
    var frictionSlip = 10.5

    @DebugProperty
    val wheelCount get() = wheels.size

    val wheels get() = getComponentsInChildren(VehicleWheel::class)

    @DebugProperty
    @Docs("Uniform getter/setter")
    @NotSerializedProperty
    var steering: Double
        get() {
            val wheels = wheels
            for (i in wheels.indices) {
                val wheel = wheels[i]
                if (wheel.steeringMultiplier != 0.0) {
                    return wheel.steering / wheel.steeringMultiplier
                }
            }
            return 0.0
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
    var engineForce: Double
        get() = wheels.firstOrNull()?.engineForce ?: 0.0
        set(value) {
            val wheels = wheels
            for (i in wheels.indices) {
                wheels[i].engineForce = value
            }
        }

    @DebugProperty
    @Docs("Uniform getter/setter")
    @NotSerializedProperty
    var brakeForce: Double
        get() = wheels.firstOrNull()?.brakeForce ?: 0.0
        set(value) {
            val wheels = wheels
            for (i in wheels.indices) {
                wheels[i].brakeForce = value
            }
        }

    init {
        mass = 1.0
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