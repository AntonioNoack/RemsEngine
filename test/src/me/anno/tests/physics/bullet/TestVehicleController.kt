package me.anno.tests.physics.bullet

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

    var accelerationStrength = 1f
    var steeringStrength = 0.5f
    var brake = 1f

    var smoothing = 5f

    @DebugProperty
    var controls = "tfgh"

    @DebugProperty
    @NotSerializedProperty
    var lastForce = 0f

    @DebugProperty
    @NotSerializedProperty
    var lastSteering = 0f

    @DebugProperty
    @NotSerializedProperty
    var lastBrake = 0f

    override fun onPhysicsUpdate(dt: Double) {

        val dt = dt.toFloat()
        val controls = controls.padEnd(4)

        var steeringSum = 0f
        if (Input.isKeyDown(controls[1])) steeringSum += steeringStrength
        if (Input.isKeyDown(controls[3])) steeringSum -= steeringStrength

        var forceSum = 0f
        if (Input.isKeyDown(controls[0])) forceSum += accelerationStrength
        if (Input.isKeyDown(controls[2])) forceSum -= accelerationStrength

        val brakeForcePerWheel = if (Input.isKeyDown(' ')) this.brake else 0f

        val factor = Maths.dtTo01(dt * smoothing)
        lastForce = Maths.mix(lastForce, forceSum, factor)
        lastSteering = Maths.mix(lastSteering, steeringSum, factor)
        lastBrake = brakeForcePerWheel

        entity?.forAllComponentsInChildren(VehicleWheel::class) {
            it.steering = lastSteering * it.steeringMultiplier
            it.brakeForce = lastBrake * it.brakeForceMultiplier
            // bullet engine refused to brake, if the motor is running
            it.engineForce = if (it.brakeForce > 0.0) 0f else lastForce * it.engineForceMultiplier
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