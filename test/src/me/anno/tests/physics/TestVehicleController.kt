package me.anno.tests.physics

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.bullet.VehicleWheel
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths

class TestVehicleController : Component(), ControlReceiver {

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

    override fun onPhysicsUpdate(): Boolean {

        val controls = controls.padEnd(4)

        var steeringSum = 0.0
        if (Input.isKeyDown(controls[1])) steeringSum += steeringStrength
        if (Input.isKeyDown(controls[3])) steeringSum -= steeringStrength

        var forceSum = 0.0
        if (Input.isKeyDown(controls[0])) forceSum += accelerationStrength
        if (Input.isKeyDown(controls[2])) forceSum -= accelerationStrength

        val brakeForcePerWheel = if (Input.isKeyDown(' ')) this.brake else 0.0

        val dt = Engine.deltaTime * smoothing
        val factor = Maths.dtTo01(dt)
        lastForce = Maths.mix(lastForce, forceSum, factor)
        lastSteering = Maths.mix(lastSteering, steeringSum, factor)
        lastBrake = brakeForcePerWheel

        entity?.anyComponentInChildren(VehicleWheel::class) {
            it.steering = lastSteering * it.steeringMultiplier
            it.brakeForce = lastBrake * it.brakeForceMultiplier
            // bullet engine refused to brake, if the motor is running
            it.engineForce = if (it.brakeForce > 0.0) 0.0 else lastForce * it.engineForceMultiplier
            false
        }
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TestVehicleController
        dst.accelerationStrength = accelerationStrength
        dst.steeringStrength = steeringStrength
        dst.brake = brake
        dst.smoothing = smoothing
    }

    override val className: String get() = "TestVehicleController"

}