package me.anno.games.carchase

import me.anno.bullet.bodies.Vehicle
import me.anno.bullet.bodies.VehicleWheel
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths

class VehicleController : Component(), InputListener, OnPhysicsUpdate {

    var accelerationStrength = 1.0
    var steeringStrength = 0.5
    var brakeStrength = 2.0

    var smoothing = 5.0

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

        var steeringSum = 0.0
        if (Input.isKeyDown(Key.KEY_A)) steeringSum += steeringStrength
        if (Input.isKeyDown(Key.KEY_D)) steeringSum -= steeringStrength

        var forceSum = 0.0
        if (Input.isKeyDown(Key.KEY_W)) forceSum += accelerationStrength
        if (Input.isKeyDown(Key.KEY_S)) forceSum -= accelerationStrength

        var brake = 0.0
        if (Input.isKeyDown(Key.KEY_SPACE)) brake += brakeStrength
        val vehicle = getComponent(Vehicle::class)
        if (vehicle != null && forceSum * vehicle.localLinearVelocityZ < 0.0) {
            brake += brakeStrength
        }

        val factor = Maths.dtTo01(dt * smoothing)
        lastForce = Maths.mix(lastForce, forceSum, factor)
        lastSteering = Maths.mix(lastSteering, steeringSum, factor)
        lastBrake = brake

        entity?.forAllComponentsInChildren(VehicleWheel::class) {
            it.steering = lastSteering * it.steeringMultiplier
            it.brakeForce = lastBrake * it.brakeForceMultiplier
            // bullet engine refused to brake, if the motor is running
            it.engineForce = if (it.brakeForce > 0.0) 0.0 else lastForce * it.engineForceMultiplier
        }
    }
}