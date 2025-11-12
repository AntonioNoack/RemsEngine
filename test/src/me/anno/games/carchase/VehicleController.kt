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

    var accelerationStrength = 1f
    var steeringStrength = 0.5f
    var brakeStrength = 2f

    var smoothing = 5f

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

        var steeringSum = 0f
        if (Input.isKeyDown(Key.KEY_A)) steeringSum += steeringStrength
        if (Input.isKeyDown(Key.KEY_D)) steeringSum -= steeringStrength

        var forceSum = 0f
        if (Input.isKeyDown(Key.KEY_W)) forceSum += accelerationStrength
        if (Input.isKeyDown(Key.KEY_S)) forceSum -= accelerationStrength

        var brake = 0f
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
            it.engineForce = if (it.brakeForce > 0f) 0f else lastForce * it.engineForceMultiplier
        }
    }
}