package me.anno.ecs.components.test

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix

class TestVehicleController : Component(), ControlReceiver {

    var force = 1.0
    var steering = 0.5
    var brake = 1.0

    var smoothing = 5.0

    @DebugProperty
    val controls = "tfgh"

    @DebugProperty
    @NotSerializedProperty
    private var lastForce = 0.0

    @DebugProperty
    @NotSerializedProperty
    private var lastSteering = 0.0

    @DebugProperty
    @NotSerializedProperty
    private var lastBrake = 0.0

    override fun onPhysicsUpdate(): Boolean {

        var steeringSum = 0.0
        if (Input.isKeyDown('f')) steeringSum += steering
        if (Input.isKeyDown('h')) steeringSum -= steering

        var forceSum = 0.0
        if (Input.isKeyDown('t')) forceSum += force
        if (Input.isKeyDown('g')) forceSum -= force

        val brakeForcePerWheel = if (Input.isKeyDown(' ')) this.brake else 0.0

        val dt = Engine.deltaTime * smoothing
        val factor = dtTo01(dt)
        lastForce = mix(lastForce, forceSum, factor)
        lastSteering = mix(lastSteering, steeringSum, factor)
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

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TestVehicleController
        clone.force = force
        clone.steering = steering
        clone.brake = brake
        clone.smoothing = smoothing
    }

    override val className get() = "TestVehicleController"

}