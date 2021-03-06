package me.anno.ecs.components.physics

import me.anno.ecs.annotations.DebugProperty
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

    @DebugProperty
    val wheelCount
        get() = wheels.size

    val wheels get() = entity!!.getComponentsInChildren(VehicleWheel::class)

    init {
        mass = 1.0
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
    }

    override val className: String = "Vehicle"

}