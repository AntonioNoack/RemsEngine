package me.anno.tests.physics.bullet

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
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Copied over from Bullet module; this had weird issues, because I messed up refactoring.
 * */
class StackOfBoxesTest {

    fun testHeavySphereKnocksOverTower() {
        // Physics setup
        val world = createWorld()
        createGround(world)

        // Tower of boxes
        val boxes = createBoxTower(world, 0.0)
        val boxSize = 1.0

        // Sphere shape and setup
        val sphereRadius = 0.5f
        val sphereMass = 50.0f // Heavy sphere
        val sphereShape = SphereShape(sphereRadius)

        val sphereTransform = Transform()
        sphereTransform.setTranslation(-5.0, (boxSize * 2 * boxes.size - 1.0), 0.0) // at height of top box

        val sphereBody = createRigidBody(sphereMass, sphereTransform, sphereShape)
        sphereBody.friction = 0.0f
        sphereBody.restitution = 0.2f // some energy loss on impact
        world.addRigidBody(sphereBody)

        val frameInWorld = Transform()
        frameInWorld.setTranslation(sphereTransform.origin) // same as sphere's origin


        // Static body representing the world (for slider reference)
        val railTransform = Transform()
        railTransform.setTranslation(10.0, 0.0, 0.0)
        val staticRail = createRigidBody(0f, railTransform, BoxShape(Vector3f(0.1f)))
        world.addRigidBody(staticRail)

        // Frame in sphere's local space (starts at origin)
        val frameInA = Transform()
        frameInA.setTranslation(railTransform.origin)

        // Frame in static body's local space — defines the rail's origin and direction
        val frameInB = Transform()
        frameInB.setTranslation(sphereTransform.origin) // matches sphere start pos

        // You can also set frameInB.basis to rotate if you want motion along a different axis
        val settings = me.anno.bullet.constraints.SliderConstraint()
        settings.lowerLinearLimit = -10.0f
        settings.upperLinearLimit = 10.0f
        settings.lowerAngleLimit = 0.0f
        settings.upperAngleLimit = 0.0f

        val slider = SliderConstraint(settings, sphereBody, staticRail, frameInA, frameInB, true)
        world.addConstraint(slider)

        // Apply initial velocity to slide the sphere along X toward the tower
        sphereBody.linearVelocity.set(10.0, 0.0, 0.0) // Move right

        runSimulation(world)
    }

    private fun runSimulation(world: DiscreteDynamicsWorld) {
        // Run the simulation
        val timeStep = 1f / 60f
        val maxSubSteps = 10
        testSceneWithUI(
            "StackOfBoxes", Entity()
                .add(BulletDebugComponent(world, timeStep, maxSubSteps))
        )
    }

    private fun createBoxTower(dynamicsWorld: DynamicsWorld, xOffsetPerBox: Double): Array<RigidBody> {
        // Boxes
        val boxSize = 1f
        val boxCount = 5
        val boxShape = BoxShape(Vector3f(boxSize))
        return Array(boxCount) { i ->
            val y = (2 * boxSize * i) + boxSize
            val x = xOffsetPerBox * i // cumulative horizontal offset
            val boxTransform = Transform()
            boxTransform.setTranslation(x, y.toDouble(), 0.0)
            val box = createRigidBody(1f, boxTransform, boxShape)
            dynamicsWorld.addRigidBody(box)
            box
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            StackOfBoxesTest().testHeavySphereKnocksOverTower()
        }

        fun createGround(dynamicsWorld: DiscreteDynamicsWorld) {
            // Ground plane
            val groundShape = StaticPlaneShape(Vector3f(0.0, 1.0, 0.0), 1.0)
            val groundBody = createRigidBody(0.0f, Vector3d(0f, -1f, 0f), groundShape)
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
