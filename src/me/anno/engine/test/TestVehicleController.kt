package me.anno.engine.test

import me.anno.ecs.Component
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input

class TestVehicleController : Component() {

    var force = 1.0

    override fun onPhysicsUpdate() {
        val vehicle = entity!!.getComponent(Vehicle::class) ?: return
        var steering = 0.0
        if (Input.isKeyDown('f')) steering--
        if (Input.isKeyDown('d')) steering++
        var force = 0.0
        if (Input.isKeyDown('t')) force++
        if (Input.isKeyDown('g')) force--
        vehicle.steering = steering
        vehicle.engineForcePerWheel = force * this.force
    }

    override fun clone(): Component {
        val clone = TestVehicleController()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TestVehicleController
        clone.force = force
    }

    override val className: String = "TestVehicleController"

}