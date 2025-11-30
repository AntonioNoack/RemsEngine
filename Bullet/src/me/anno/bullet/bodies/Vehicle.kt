package me.anno.bullet.bodies

import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty

class Vehicle : DynamicBody() {

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
}