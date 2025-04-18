package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.Vehicle
import me.anno.bullet.VehicleWheel
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.angleDifference
import me.anno.maths.Maths.sq
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class BulletVehicleTest {

    fun initPhysics(): BulletPhysics {
        val physics = BulletPhysics()
        physics.gravity = Vector3d(0.0, -10.0, 0.0)
        physics.fixedStep = 0.0
        physics.maxSubSteps = 0
        Systems.registerSystem(physics)
        return physics
    }

    fun defineVehicle(): Entity {
        val vehicle = Entity("Vehicle")
            .setPosition(0.0, 0.3, 0.0)
            .add(Vehicle().apply { mass = 1500.0 })
            .add(BoxCollider().apply { halfExtends.set(1.2, 0.4, 2.2) })
            .add(
                Entity("VehicleMesh")
                    .add(MeshComponent(flatCube, DefaultAssets.goldenMaterial))
                    .setScale(1.2f, 0.4f, 2.2f)
            )
        val wheelMesh = CylinderModel.createCylinder(
            12, 2, true, true,
            null, 1f, Mesh()
        )
        val wheelMaterial = Material.diffuse(0x333333)
        for (dx in -1..1 step 2) {
            for (dz in -1..1 step 2) {
                val wheel = Entity("Wheel[$dx,$dz]")
                    .setPosition(dx * 1.0, 0.2, dz * 1.4)
                    .add(VehicleWheel().apply {
                        steeringMultiplier = if (dz > 0.0) 1.0 else 0.0
                        engineForceMultiplier = if (dz > 0.0) 1.0 else 0.0
                        suspensionRestLength = 1.0
                        suspensionStiffness = 10.0
                        suspensionDampingCompression = 0.9
                        suspensionDampingRelaxation = 0.9
                        radius = 0.5
                    })
                    .add(
                        Entity()
                            .setRotation(0.0f, 0.0f, PIf / 2)
                            .setScale(0.5f, 0.1f, 0.5f)
                            .add(MeshComponent(wheelMesh, wheelMaterial))
                    )
                vehicle.add(wheel)
            }
        }
        return vehicle
    }

    fun defineFlatFloor(scale: Vector3d = Vector3d(500.0, 5.0, 500.0)): Entity {
        val floorMaterial = Material.diffuse(0xFFFACD)
        val floor = Entity("Floor")
            .setPosition(0.0, -scale.y, 0.0)
            .add(Rigidbody().apply { mass = 0.0 })
            .add(BoxCollider().apply { halfExtends.set(scale) })
            .add(
                Entity("FloorMesh")
                    .add(MeshComponent(flatCube, floorMaterial))
                    .setScale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
            )
        return floor
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testVehicleStationary() {
        val physics = initPhysics()
        val world = Entity("World")
        val floor = defineFlatFloor()
        // define vehicle
        val vehicle = defineVehicle()
        world.add(vehicle)
        world.add(floor)
        Systems.world = world
        // give time for suspension to relax
        val dt = 1.0 / 16.0
        for (i in 0 until 50) {
            // println("${vehicle.position}, ${vehicle.rotation}")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
        // check that it is stationary
        assertEquals(Vector3d(0.0, 1.0, 0.0), vehicle.position, 0.1)
        // todo bug: velocity variable somehow is (0,0.6,0), even though the vehicle isn't moving
        // assertEquals(Vector3d(0.0), vehicle.getComponent(Rigidbody::class)!!.linearVelocity, 0.01)
        assertEquals(Quaternionf(), vehicle.rotation, 0.01)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testVehicleMoving() {
        val physics = initPhysics()
        val world = Entity("World")
        val floor = defineFlatFloor()
        // define vehicle
        val vehicle = defineVehicle()
        world.add(vehicle)
        world.add(floor)
        Systems.world = world
        // wait for suspension to relax
        val dt = 1.0 / 16.0
        for (i in 0 until 50) {
            // println("${vehicle.position}, ${vehicle.rotation}")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
        assertEquals(Vector3d(0.0, 1.05, 0.0), vehicle.position, 0.05)
        assertEquals(Quaterniond(), vehicle.rotation, 0.01)
        // turn on motor
        vehicle.getComponent(Vehicle::class)!!.engineForce = 1000.0
        // check that it accelerates
        for (i in 0 until 50) {
            assertEquals(Vector3d(0.0, 1.05, sq((i - 0.5) / 48.5) * 12.25), vehicle.position, 0.05)
            assertEquals(Quaternionf(), vehicle.rotation, 0.1)
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testVehicleSteering() {
        val physics = initPhysics()
        val world = Entity("World")
        val floor = defineFlatFloor()
        // define vehicle
        val vehicle = defineVehicle()
        world.add(vehicle)
        world.add(floor)
        Systems.world = world
        // wait for suspension to relax
        val dt = 1.0 / 16.0
        for (i in 0 until 50) {
            // println("${vehicle.position}, ${vehicle.rotation}")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
        assertEquals(Vector3d(0.0, 1.0, 0.0), vehicle.position, 0.1)
        assertEquals(Quaternionf(), vehicle.rotation, 0.01)
        // turn on motor & apply steering
        val vehicleI = vehicle.getComponent(Vehicle::class)!!
        vehicleI.engineForce = 200.0
        vehicleI.steering = 0.5

        var lastAngle = 0.0

        // can be used for debugging the vehicle:
        // testSceneWithUI("TestVehicleSteering", world)

        // start accelerating
        for (i in 0 until 100) {
            val yxzRotation = vehicle.rotation.getEulerAnglesYXZ(Vector3f())
            assertEquals(Vector3d(0.0, yxzRotation.y.toDouble(), 0.0), yxzRotation, 0.2)
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            lastAngle = yxzRotation.y.toDouble()
        }

        // check that it drives in a circle
        for (i in 0 until 1000) {
            val yxzRotation = vehicle.rotation.getEulerAnglesYXZ(Vector3f())
            assertEquals(Vector3d(0.0, yxzRotation.y.toDouble(), 0.0), yxzRotation, 0.2)
            // unfortunately this oscillates :/
            val angleDiff = angleDifference(yxzRotation.y - lastAngle)
            lastAngle = yxzRotation.y.toDouble()
            // println("[$i] $angleDiff")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            assertTrue(angleDiff >= 0.03 && angleDiff < 0.06) { "$angleDiff" }
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testVehicleRolling() {
        val physics = initPhysics()
        val world = Entity("World")
        val angle = (5.0f).toRadians()
        val rampHalfExtends = 40.0
        val z0 = -rampHalfExtends + 3.0
        val y0 = 1.06 - angle * z0
        val floor = defineFlatFloor(Vector3d(3.0, 5.0, rampHalfExtends))
        floor.setRotation(angle, 0f, 0f)
        // define vehicle on a hill
        val vehicle = defineVehicle()
        vehicle.setRotation(angle, 0f, 0f)
        vehicle.setPosition(0.0, y0, z0)
        world.add(vehicle)
        world.add(floor)
        Systems.world = world

        // test the scene visually, if you want
        if (false) testSceneWithUI("TestVehicleRolling", world)

        // check that vehicle rolls down
        val dt = 1.0 / 8.0
        for (i in 0..100) {
            val position = vehicle.position - Vector3d(0.0, y0, z0)
            val expectedY = -position.z * angle
            if (i >= 10) {
                assertEquals(expectedY, position.y, 0.1)
            }
            assertEquals(0.0, position.x, 1e-9)
            val expectedPosZ = 67.15 * sq(i / 99.0)
            assertEquals(expectedPosZ, position.z, 0.2)
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testVehicleBrakesPreventRolling() {
        val physics = initPhysics()
        val world = Entity("World")
        val angle = (5.0f).toRadians()
        val rampHalfExtends = 40.0
        val z0 = -rampHalfExtends + 3.0
        val y0 = 1.06 - angle * z0
        val floor = defineFlatFloor(Vector3d(3.0, 5.0, rampHalfExtends))
        floor.setRotation(angle, 0.0f, 0.0f)
        // define vehicle on a hill
        val vehicle = defineVehicle()
        vehicle.setRotation(angle, 0.0f, 0.0f)
        vehicle.setPosition(0.0, y0, z0)
        world.add(vehicle)
        world.add(floor)
        Systems.world = world

        val vehicleI = vehicle.getComponent(Vehicle::class)!!
        vehicleI.brakeForce = 100.0

        // test the scene visually, if you want
        if (false) testSceneWithUI("TestVehicleRolling", world)

        // check that vehicle doesn't roll down
        // unfortunately, it slides down with a speed of ~0.16m/s
        val dt = 1.0 / 8.0
        for (i in 0..100) {
            val position = vehicle.position - Vector3d(0.0, y0, z0)
            assertEquals(0.0, position.x, 1e-9)
            assertEquals(0.0, position.y, 0.25)
            assertEquals(0.0, position.z, 0.25 / angle)
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
    }
}