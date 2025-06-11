package com.bulletphysics

import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import cz.advel.stack.Stack.Companion.reset
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.joml.Vector3d
import kotlin.math.abs

class VehicleTest {
    private fun createVehicle(world: DiscreteDynamicsWorld, startPos: Vector3d): RaycastVehicle {
        val chassisShape: CollisionShape = BoxShape(Vector3d(1.0, 0.5, 2.0)) // Simple box car

        val chassis: RigidBody =
            StackOfBoxesTest.Companion.createRigidBody(800f, startPos, chassisShape) // Heavy for stability
        world.addRigidBody(chassis)

        val tuning = VehicleTuning()
        tuning.suspensionStiffness = 20.0
        tuning.suspensionDamping = 2.3
        tuning.suspensionCompression = 4.4
        tuning.frictionSlip = 1000.0
        tuning.maxSuspensionTravelCm = 500.0

        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(tuning, chassis, raycaster)
        chassis.setActivationStateMaybe(CollisionObject.DISABLE_DEACTIVATION)

        world.addVehicle(vehicle)

        // Add 4 wheels
        val wheelDirection = Vector3d(0.0, -1.0, 0.0)
        val wheelAxle = Vector3d(-1.0, 0.0, 0.0)
        val suspensionRestLength = 0.6f
        val wheelRadius = 0.5f
        val isFront = true

        // Positions relative to chassis
        vehicle.addWheel(
            Vector3d(1.0, -0.5, 2.0),
            wheelDirection,
            wheelAxle,
            suspensionRestLength.toDouble(),
            wheelRadius.toDouble(),
            tuning,
            isFront
        )
        vehicle.addWheel(
            Vector3d(-1.0, -0.5, 2.0),
            wheelDirection,
            wheelAxle,
            suspensionRestLength.toDouble(),
            wheelRadius.toDouble(),
            tuning,
            isFront
        )
        vehicle.addWheel(
            Vector3d(1.0, -0.5, -2.0),
            wheelDirection,
            wheelAxle,
            suspensionRestLength.toDouble(),
            wheelRadius.toDouble(),
            tuning,
            false
        )
        vehicle.addWheel(
            Vector3d(-1.0, -0.5, -2.0),
            wheelDirection,
            wheelAxle,
            suspensionRestLength.toDouble(),
            wheelRadius.toDouble(),
            tuning,
            false
        )

        return vehicle
    }

    private fun normalize(d: Vector3d): Vector3d {
        d.normalize()
        return d
    }

    @Test
    fun testVehicleBehavior() {
        // Step 1: Flat ground test
        var world: DiscreteDynamicsWorld = StackOfBoxesTest.Companion.createWorld()
        createGroundPlane(Vector3d(0.0, 1.0, 0.0), world) // Flat plane
        var vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))

        simulate(world, 120)
        val flatPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(flatPos)
        Assertions.assertTrue(flatPos.y < 1.5f && flatPos.y > 0.5f, "Vehicle should rest on ground")

        // Step 2: Hill test (no engine force)
        world = StackOfBoxesTest.Companion.createWorld()
        createGroundPlane(normalize(Vector3d(0.0, 1.0, -0.5)), world) // Inclined
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))

        simulate(world, 240) // 4 seconds
        val slopePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(slopePos)
        Assertions.assertTrue(slopePos.z < -1f, "Vehicle should have rolled downhill")

        // Step 3: Driving test
        world = StackOfBoxesTest.Companion.createWorld()
        createGroundPlane(Vector3d(0.0, 1.0, 0.0), world) // Flat
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f) // Drive forward

        simulate(world, 240)
        val drivePos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(drivePos)
        Assertions.assertTrue(drivePos.z > 1f, "Vehicle should drive forward")

        // Step 4: Driving downhill
        world = StackOfBoxesTest.Companion.createWorld()
        createGroundPlane(normalize(Vector3d(0.0, 1.0, 0.2)), world) // Mild hill
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f)

        simulate(world, 240)
        val downPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(downPos)
        Assertions.assertTrue(downPos.z > 2f, "Vehicle should drive faster downhill")

        // Step 5: Turning
        world = StackOfBoxesTest.Companion.createWorld()
        createGroundPlane(Vector3d(0.0, 1.0, 0.0), world) // Flat again
        vehicle = createVehicle(world, Vector3d(0f, 2f, 0f))
        applyEngineForce(vehicle, 800f)
        applySteering(vehicle, 0.3f) // Turn wheels slightly

        simulate(world, 240)
        val turnPos = Vector3d()
        vehicle.rigidBody.getCenterOfMassPosition(turnPos)
        Assertions.assertTrue(abs(turnPos.x) > 0.5f, "Vehicle should have turned")
    }

    @Test
    fun testVehicleBrakingDownhill() {
        val world: DiscreteDynamicsWorld = StackOfBoxesTest.Companion.createWorld()

        // Create downhill plane (slope in +Z)
        createGroundPlane(normalize(Vector3d(0.0, 1.0, 0.2)), world)

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
        Assertions.assertTrue(deltaZ < 0.7f, "Vehicle should slow down when braking downhill")
    }

    private fun applyBrakes(vehicle: RaycastVehicle, brakeForce: Float) {
        for (i in 0 until vehicle.numWheels) {
            vehicle.setBrake(brakeForce.toDouble(), i)
        }
    }

    private fun applyEngineForce(vehicle: RaycastVehicle, force: Float) {
        for (i in 0 until vehicle.numWheels) {
            vehicle.applyEngineForce(force.toDouble(), i)
        }
    }

    private fun applySteering(vehicle: RaycastVehicle, steering: Float) {
        // Only front wheels
        vehicle.setSteeringValue(steering.toDouble(), 0)
        vehicle.setSteeringValue(steering.toDouble(), 1)
    }

    private fun simulate(world: DiscreteDynamicsWorld, steps: Int) {
        for (i in 0 until steps) {
            world.stepSimulation((1f / 60f).toDouble(), 10)
            reset(false)
        }
    }

    private fun createGroundPlane(normal: Vector3d, world: DiscreteDynamicsWorld) {
        val planeShape: CollisionShape = StaticPlaneShape(normal, 0f.toDouble())
        val ground: RigidBody = StackOfBoxesTest.Companion.createRigidBody(0f, Vector3d(0f, 0f, 0f), planeShape)
        world.addRigidBody(ground)
    }
}
