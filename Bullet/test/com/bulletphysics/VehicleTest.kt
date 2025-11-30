package com.bulletphysics

import com.bulletphysics.StackOfBoxesTest.Companion.createRigidBody
import com.bulletphysics.StackOfBoxesTest.Companion.createWorld
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import cz.advel.stack.Stack.Companion.reset
import me.anno.bullet.bodies.VehicleWheel
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.abs

class VehicleTest {
    private fun createVehicle(world: DiscreteDynamicsWorld, startPos: Vector3d): RaycastVehicle {
        val chassisShape = BoxShape(Vector3f(1.0, 0.5, 2.0)) // Simple box car

        val chassis = createRigidBody(800f, startPos, chassisShape) // Heavy for stability
        world.addRigidBody(chassis)

        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(chassis, raycaster)
        chassis.setActivationStateMaybe(ActivationState.ALWAYS_ACTIVE)
        vehicle.setCoordinateSystem(0, 2, 1)

        world.addVehicle(vehicle)

        // Add 4 wheels
        val wheelDirection = Vector3f(0.0, -1.0, 0.0)
        val wheelAxle = Vector3f(-1.0, 0.0, 0.0)

        fun tuning(): VehicleWheel {
            val tuning = VehicleWheel()
            tuning.suspensionStiffness = 20.0f
            tuning.suspensionDampingRelaxation = 2.3f
            tuning.suspensionDampingCompression = 4.4f
            tuning.frictionSlip = 1000.0f
            tuning.maxSuspensionTravel = 5.0f
            return tuning
        }

        // Positions relative to chassis
        vehicle.addWheel(
            Vector3f(1.0, -0.5, 2.0),
            wheelDirection, wheelAxle, tuning(), 0L,
        )
        vehicle.addWheel(
            Vector3f(-1.0, -0.5, 2.0),
            wheelDirection, wheelAxle, tuning(), 0L,
        )
        vehicle.addWheel(
            Vector3f(1.0, -0.5, -2.0),
            wheelDirection, wheelAxle, tuning(), 0L,
        )
        vehicle.addWheel(
            Vector3f(-1.0, -0.5, -2.0),
            wheelDirection, wheelAxle, tuning(), 0L,
        )
        return vehicle
    }

    private fun normalize(d: Vector3f): Vector3f {
        d.normalize()
        return d
    }

    @Test
    fun testVehicleBehavior() {
        // Step 1: Flat ground test
        var world = createWorld()
        createGroundPlane(Vector3f(0.0, 1.0, 0.0), world) // Flat plane
        var vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))

        simulate(world, 120)
        val flatPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(flatPos)
        assertTrue(flatPos.y < 1.5f && flatPos.y > 0.5f, "Vehicle should rest on ground")

        // Step 2: Hill test (no engine force)
        world = createWorld()
        createGroundPlane(normalize(Vector3f(0.0, 1.0, -0.5)), world) // Inclined
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))

        simulate(world, 240) // 4 seconds
        val slopePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(slopePos)
        assertTrue(slopePos.z < -1f, "Vehicle should have rolled downhill")

        // Step 3: Driving test
        world = createWorld()
        createGroundPlane(Vector3f(0.0, 1.0, 0.0), world) // Flat
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f) // Drive forward

        simulate(world, 240)
        val drivePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(drivePos)
        assertTrue(drivePos.z > 1f, "Vehicle should drive forward")

        // Step 4: Driving downhill
        world = createWorld()
        createGroundPlane(normalize(Vector3f(0.0, 1.0, 0.2)), world) // Mild hill
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f)

        simulate(world, 240)
        val downPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(downPos)
        assertTrue(downPos.z > 2f, "Vehicle should drive faster downhill")

        // Step 5: Turning
        world = createWorld()
        createGroundPlane(Vector3f(0.0, 1.0, 0.0), world) // Flat again
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f)
        applySteering(vehicle, 0.3f) // Turn wheels slightly

        simulate(world, 240)
        val turnPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(turnPos)
        assertTrue(abs(turnPos.x) > 0.5f, "Vehicle should have turned")
    }

    @Test
    fun testVehicleBrakingDownhill() {
        val world = createWorld()

        // Create downhill plane (slope in +Z)
        createGroundPlane(normalize(Vector3f(0.0, 1.0, 0.2)), world)

        // Create vehicle
        val vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))

        // Phase 1: Drive downhill for 2 seconds
        applyEngineForce(vehicle, 800f)
        simulate(world, 120) // 2 seconds
        val preBrakePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(preBrakePos)

        println("Distance traveled during driving phase: deltaZ = " + preBrakePos.z)

        // Phase 2: Apply brakes, no more engine force
        applyEngineForce(vehicle, 0f)
        applyBrakes(vehicle, 200f) // Strong braking force
        simulate(world, 120) // 2 more seconds
        val postBrakePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(postBrakePos)

        val deltaZ = postBrakePos.z - preBrakePos.z

        println("Distance traveled during brake phase: deltaZ = $deltaZ")

        // Assert that braking significantly reduced forward motion
        assertTrue(deltaZ < 0.7f, "Vehicle should slow down when braking downhill")
    }

    private fun applyBrakes(vehicle: RaycastVehicle, brakeForce: Float) {
        for (i in 0 until vehicle.numWheels) {
            vehicle.setBrake(brakeForce, i)
        }
    }

    private fun applyEngineForce(vehicle: RaycastVehicle, force: Float) {
        for (i in 0 until vehicle.numWheels) {
            vehicle.applyEngineForce(force, i)
        }
    }

    private fun applySteering(vehicle: RaycastVehicle, steering: Float) {
        // Only front wheels
        vehicle.setSteeringValue(steering, 0)
        vehicle.setSteeringValue(steering, 1)
    }

    private fun RaycastVehicle.setSteeringValue(steering: Float, wheel: Int) {
        wheels[wheel].config.steering = steering
    }

    private fun RaycastVehicle.applyEngineForce(force: Float, wheel: Int) {
        wheels[wheel].config.engineForce = force
    }

    private fun RaycastVehicle.setBrake(brake: Float, wheelIndex: Int) {
        wheels[wheelIndex].config.brakeForce = brake
    }

    private fun simulate(world: DiscreteDynamicsWorld, steps: Int) {
        for (i in 0 until steps) {
            world.stepSimulation((1f / 60f), 10)
            reset(false)
        }
    }

    private fun createGroundPlane(normal: Vector3f, world: DiscreteDynamicsWorld) {
        val planeShape: CollisionShape = StaticPlaneShape(normal, 0.0)
        val ground: RigidBody = createRigidBody(0f, Vector3d(0f, 0f, 0f), planeShape)
        world.addRigidBody(ground)
    }
}
