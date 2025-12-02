package com.bulletphysics

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.abs

class StackOfBoxesTest {

    @Test
    fun testStackDoesNotFall() {
        val world = createWorld()
        createGround(world)

        val boxes = createBoxTower(world, 0f)
        runSimulation(world)

        // Validate stack has not fallen
        for (i in boxes.indices) {
            val trans = boxes[i]!!.worldTransform
            val y = trans.origin.y
            println(trans.origin)
            assertTrue(abs(trans.origin.x) < 0.5f, "Box $i fell sideways on X axis")
            assertTrue(abs(trans.origin.z) < 0.5f, "Box $i fell sideways on Z axis")
            assertTrue(y > 0.1f, "Box $i dropped too low")
        }
    }

    @Test
    fun testStackFallsOverWhenOffset() {
        val world: DiscreteDynamicsWorld = createWorld()
        createGround(world)

        val boxes = createBoxTower(world, 0.6f)
        runSimulation(world)

        // Check that the top box has significantly deviated horizontally (i.e., tower has fallen)
        val topBoxTransform = boxes[boxes.size - 1]!!.worldTransform
        val finalX = topBoxTransform.origin.x
        val finalZ = topBoxTransform.origin.z

        // Threshold assumes a fall if final X or Z is way off from initial offset
        val fellOver = abs(finalX) > 3.0f || abs(finalZ) > 1.0f
        assertTrue(fellOver, "The stack did not fall over as expected. Final X: $finalX, Z: $finalZ")
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
        val sphereShape = SphereShape(sphereRadius)

        val sphereTransform = Transform()
        sphereTransform.setIdentity()
        sphereTransform.setTranslation(-5.0, (boxSize * 2 * boxes.size - 1f).toDouble(), 0.0) // at height of top box

        val sphereBody = createRigidBody(sphereMass, sphereTransform, sphereShape)
        sphereBody.friction = 0.0f
        sphereBody.restitution = 0.2f // some energy loss on impact
        world.addRigidBody(sphereBody)

        val frameInWorld = Transform()
        frameInWorld.setIdentity()
        frameInWorld.setTranslation(sphereTransform.origin) // same as sphere's origin


        // Static body representing the world (for slider reference)
        val staticRail = createRigidBody(0f, Transform(), BoxShape(Vector3f(0.1f)))
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
        val settings = me.anno.bullet.constraints.SliderConstraint()
        val slider = SliderConstraint(settings,sphereBody, staticRail, frameInA, frameInB, true)
        settings.lowerLinearLimit = -10.0f
        settings.upperLinearLimit = 10.0f
        settings.lowerAngleLimit = 0.0f
        settings.upperAngleLimit = 0.0f
        world.addConstraint(slider)

        // Apply initial velocity to slide the sphere along X toward the tower
        sphereBody.linearVelocity.set(10.0, 0.0, 0.0) // Move right

        runSimulation(world)

        // Verify the tower has fallen (by checking top box's horizontal deviation)
        val topTransform = boxes[boxes.size - 1]!!.worldTransform
        val dx = abs(topTransform.origin.x)
        val dz = abs(topTransform.origin.z)

        val towerFell = dx > 1.0f || dz > 1.0f
        assertTrue(towerFell, "Tower did not fall after being hit. X offset: $dx, Z offset: $dz")
    }

    private fun runSimulation(dynamicsWorld: DiscreteDynamicsWorld) {
        // Run the simulation
        val timeStep = 1f / 60f
        val maxSubSteps = 10
        val steps = 240 // 4 seconds of simulation
        repeat(steps) {
            dynamicsWorld.stepSimulation(timeStep, maxSubSteps)
        }
    }

    private fun createBoxTower(dynamicsWorld: DynamicsWorld, xOffsetPerBox: Float): Array<RigidBody?> {
        // Boxes
        val boxSize = 1f
        val boxCount = 5
        val boxes = arrayOfNulls<RigidBody>(boxCount)
        val boxShape: CollisionShape = BoxShape(Vector3f(boxSize))

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

        @JvmStatic
        fun main(args: Array<String>) {
            StackOfBoxesTest().testHeavySphereKnocksOverTower()
        }

        fun createGround(dynamicsWorld: DiscreteDynamicsWorld) {
            // Ground plane
            val groundShape = StaticPlaneShape(Vector3f(0.0, 1.0, 0.0), 1.0)
            val groundBody = createRigidBody(0f, Vector3d(0f, -1f, 0f), groundShape)
            dynamicsWorld.addRigidBody(groundBody)
        }

        fun createWorld(): DiscreteDynamicsWorld {
            // Physics setup
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val broadphase = DbvtBroadphase()
            val solver = SequentialImpulseConstraintSolver()
            val world = DiscreteDynamicsWorld(dispatcher, broadphase, solver)

            world.setGravity(Vector3f(0.0, -10.0, 0.0))
            return world
        }

        fun createRigidBody(mass: Float, transform: Transform, shape: CollisionShape): RigidBody {
            val localInertia = Vector3f(0.0, 0.0, 0.0)
            if (mass != 0f) {
                shape.calculateLocalInertia(mass, localInertia)
            }

            val body = RigidBody(mass, shape, localInertia)
            body.setInitialTransform(transform)
            return body
        }

        fun createRigidBody(mass: Float, position: Vector3d, shape: CollisionShape): RigidBody {
            val tf = Transform()
            tf.setIdentity()
            tf.setTranslation(position)
            return createRigidBody(mass, tf, shape)
        }
    }
}
