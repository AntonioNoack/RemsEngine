package me.anno.ecs.components.physics

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.SerializedProperty

class Vehicle : Rigidbody() {

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

    var engineForcePerWheel = 0.0

    var steering = 0.0

    var brakeForcePerWheel = 0.0

    val wheels get() = entity!!.getComponents(VehicleWheel::class)

    override fun onPhysicsUpdate(): Boolean {
        // only if enabled...
        entity!!.anyComponent(VehicleWheel::class) {
            it.steering = steering
            it.engineForce = engineForcePerWheel
            it.brakeForce = brakeForcePerWheel
            false
        }
        return true
    }

    override fun clone(): Vehicle {
        val clone = Vehicle()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Vehicle
        clone.suspensionDamping = suspensionDamping
        clone.suspensionStiffness = suspensionStiffness
        clone.suspensionCompression = suspensionCompression
        clone.maxSuspensionTravelCm = maxSuspensionTravelCm
        clone.frictionSlip = frictionSlip
        clone.steering = steering
        clone.engineForcePerWheel = engineForcePerWheel
        clone.brakeForcePerWheel = brakeForcePerWheel
    }

    override val className: String = "Vehicle"

}