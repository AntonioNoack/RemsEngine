package com.bulletphysics

import com.bulletphysics.collision.broadphase.AxisSweep3
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.joml.Vector3d
import kotlin.math.abs

class StackOfBoxesTest {
    @Test
    fun testStackDoesNotFall() {
        val world: DiscreteDynamicsWorld = createWorld()
        createGround(world)

        val boxes = createBoxTower(world, 0f)
        runSimulation(world)

        // Validate stack has not fallen
        for (i in boxes.indices) {
            val trans = Transform()
            boxes[i]!!.motionState!!.getWorldTransform(trans)
            val y = trans.origin.y
            println(trans.origin)
            Assertions.assertTrue(abs(trans.origin.x) < 0.5f, "Box " + i + " fell sideways on X axis")
            Assertions.assertTrue(abs(trans.origin.z) < 0.5f, "Box " + i + " fell sideways on Z axis")
            Assertions.assertTrue(y > 0.1f, "Box " + i + " dropped too low")
        }
    }

    @Test
    fun testStackFallsOverWhenOffset() {
        val world: DiscreteDynamicsWorld = createWorld()
        createGround(world)

        val boxes = createBoxTower(world, 0.6f)
        runSimulation(world)

        // Check that the top box has significantly deviated horizontally (i.e., tower has fallen)
        val topBoxTransform = Transform()
        boxes[boxes.size - 1]!!.motionState!!.getWorldTransform(topBoxTransform)
        val finalX = topBoxTransform.origin.x
        val finalZ = topBoxTransform.origin.z

        // Threshold assumes a fall if final X or Z is way off from initial offset
        val fellOver = abs(finalX) > 3.0f || abs(finalZ) > 1.0f
        Assertions.assertTrue(
            fellOver,
            "The stack did not fall over as expected. Final X: " + finalX + ", Z: " + finalZ
        )
    }

    @Test
    fun testHeavySphereKnocksOverTower() {
        // Physics setup
        val world: DiscreteDynamicsWorld = createWorld()
        createGround(world)

        // Tower of boxes
        val boxes = createBoxTower(world, 0f)
        val boxSize = 1f

        // Sphere shape and setup
        val sphereRadius = 0.5f
        val sphereMass = 50f // Heavy sphere
        val sphereShape: CollisionShape = SphereShape(sphereRadius.toDouble())

        val sphereTransform = Transform()
        sphereTransform.setIdentity()
        sphereTransform.setTranslation(-5.0, (boxSize * 2 * boxes.size - 1f).toDouble(), 0.0) // at height of top box

        val sphereBody: RigidBody = createRigidBody(sphereMass, sphereTransform, sphereShape)
        sphereBody.friction = 0.0
        sphereBody.restitution = 0.2 // some energy loss on impact
        world.addRigidBody(sphereBody)

        val frameInWorld = Transform()
        frameInWorld.setIdentity()
        frameInWorld.setTranslation(sphereTransform.origin) // same as sphere's origin


        // Static body representing the world (for slider reference)
        val staticRail: RigidBody = createRigidBody(0f, Transform(), BoxShape(Vector3d(0.1, 0.1, 0.1)))
        world.addRigidBody(staticRail)

        // Frame in sphere's local space (starts at origin)
        val frameInA = Transform()
        frameInA.setIdentity()
        frameInA.setTranslation(0.0, 0.0, 0.0)

        // Frame in static body's local space — defines the rail's origin and direction
        val frameInB = Transform()
        frameInB.setIdentity()
        frameInB.setTranslation(sphereTransform.origin) // matches sphere start pos

        // You can also set frameInB.basis to rotate if you want motion along a different axis
        val slider = SliderConstraint(sphereBody, staticRail, frameInA, frameInB, true)
        slider.lowerLinearLimit = -10.0
        slider.upperLinearLimit = 10.0
        slider.lowerAngularLimit = 0.0
        slider.upperAngularLimit = 0.0
        world.addConstraint(slider)

        // Apply initial velocity to slide the sphere along X toward the tower
        sphereBody.setLinearVelocity(Vector3d(10.0, 0.0, 0.0)) // Move right

        runSimulation(world)

        // Verify the tower has fallen (by checking top box's horizontal deviation)
        val topTransform = Transform()
        boxes[boxes.size - 1]!!.motionState!!.getWorldTransform(topTransform)
        val dx = abs(topTransform.origin.x)
        val dz = abs(topTransform.origin.z)

        val towerFell = dx > 1.0f || dz > 1.0f
        Assertions.assertTrue(towerFell, "Tower did not fall after being hit. X offset: " + dx + ", Z offset: " + dz)
    }

    private fun runSimulation(dynamicsWorld: DiscreteDynamicsWorld) {
        // Run the simulation
        val timeStep = 1f / 60f
        val maxSubSteps = 10
        val steps = 240 // 4 seconds of simulation
        for (i in 0 until steps) {
            dynamicsWorld.stepSimulation(timeStep.toDouble(), maxSubSteps)
        }
    }

    private fun createBoxTower(dynamicsWorld: DynamicsWorld, xOffsetPerBox: Float): Array<RigidBody?> {
        // Boxes
        val boxSize = 1f
        val boxCount = 5
        val boxes = arrayOfNulls<RigidBody>(boxCount)
        val boxShape: CollisionShape = BoxShape(Vector3d(boxSize.toDouble(), boxSize.toDouble(), boxSize.toDouble()))

        for (i in 0 until boxCount) {
            val boxTransform = Transform()
            boxTransform.setIdentity()
            val y = (2 * boxSize * i) + boxSize
            val x = xOffsetPerBox * i // cumulative horizontal offset
            boxTransform.setTranslation(x.toDouble(), y.toDouble(), 0.0)
            boxes[i] = createRigidBody(1f, boxTransform, boxShape)
            dynamicsWorld.addRigidBody(boxes[i]!!)
        }
        return boxes
    }

    companion object {
        fun createGround(dynamicsWorld: DiscreteDynamicsWorld) {
            // Ground plane
            val groundShape: CollisionShape = StaticPlaneShape(Vector3d(0.0, 1.0, 0.0), 1.0)
            val groundBody: RigidBody = createRigidBody(0f, Vector3d(0f, -1f, 0f), groundShape)
            dynamicsWorld.addRigidBody(groundBody)
        }

        fun createWorld(): DiscreteDynamicsWorld {
            // Physics setup
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val worldAabbMin = Vector3d(-1000.0, -1000.0, -1000.0)
            val worldAabbMax = Vector3d(1000.0, 1000.0, 1000.0)
            val broadphase = AxisSweep3(worldAabbMin, worldAabbMax)
            val solver = SequentialImpulseConstraintSolver()
            val world = DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig)

            world.setGravity(Vector3d(0.0, -10.0, 0.0))
            return world
        }

        fun createRigidBody(mass: Float, transform: Transform, shape: CollisionShape): RigidBody {
            val localInertia = Vector3d(0.0, 0.0, 0.0)
            if (mass != 0f) {
                shape.calculateLocalInertia(mass.toDouble(), localInertia)
            }

            val motionState = DefaultMotionState(transform)
            val rbInfo = RigidBodyConstructionInfo(mass.toDouble(), motionState, shape, localInertia)
            return RigidBody(rbInfo)
        }

        fun createRigidBody(mass: Float, position: Vector3d, shape: CollisionShape): RigidBody {
            val tf = Transform()
            tf.setIdentity()
            tf.setTranslation(position)
            return createRigidBody(mass, tf, shape)
        }
    }
}
