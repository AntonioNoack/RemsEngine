package me.anno.tests.physics

import me.anno.bullet.bodies.VehicleWheel
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.input.Input
import me.anno.maths.Maths

class TestVehicleController : Component(), InputListener, OnPhysicsUpdate {

    var accelerationStrength = 1.0
    var steeringStrength = 0.5
    var brake = 1.0

    var smoothing = 5.0

    @DebugProperty
    var controls = "tfgh"

    @DebugProperty
    @NotSerializedProperty
    var lastForce = 0.0

    @DebugProperty
    @NotSerializedProperty
    var lastSteering = 0.0

    @DebugProperty
    @NotSerializedProperty
    var lastBrake = 0.0

    override fun onPhysicsUpdate(dt: Double) {

        val controls = controls.padEnd(4)

        var steeringSum = 0.0
        if (Input.isKeyDown(controls[1])) steeringSum += steeringStrength
        if (Input.isKeyDown(controls[3])) steeringSum -= steeringStrength

        var forceSum = 0.0
        if (Input.isKeyDown(controls[0])) forceSum += accelerationStrength
        if (Input.isKeyDown(controls[2])) forceSum -= accelerationStrength

        val brakeForcePerWheel = if (Input.isKeyDown(' ')) this.brake else 0.0

        val factor = Maths.dtTo01(dt * smoothing)
        lastForce = Maths.mix(lastForce, forceSum, factor)
        lastSteering = Maths.mix(lastSteering, steeringSum, factor)
        lastBrake = brakeForcePerWheel

        entity?.forAllComponentsInChildren(VehicleWheel::class) {
            it.steering = lastSteering * it.steeringMultiplier
            it.brakeForce = lastBrake * it.brakeForceMultiplier
            // bullet engine refused to brake, if the motor is running
            it.engineForce = if (it.brakeForce > 0.0) 0.0 else lastForce * it.engineForceMultiplier
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TestVehicleController) return
        dst.accelerationStrength = accelerationStrength
        dst.steeringStrength = steeringStrength
        dst.brake = brake
        dst.smoothing = smoothing
    }
}