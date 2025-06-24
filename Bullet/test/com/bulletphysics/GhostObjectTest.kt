package com.bulletphysics

import com.bulletphysics.collision.broadphase.CollisionFilterGroups
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.GhostPairCallback
import com.bulletphysics.collision.dispatch.PairCachingGhostObject
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.joml.Vector3d

class GhostObjectTest {
    @Test
    fun testGhostObjectOverlapDetection() {
        val world: DiscreteDynamicsWorld = StackOfBoxesTest.Companion.createWorld()
        world.broadphase.overlappingPairCache.setInternalGhostPairCallback(GhostPairCallback())
        StackOfBoxesTest.Companion.createGround(world)

        // Ghost object region (box shape)
        val ghostShape = BoxShape(Vector3d(1.0, 0.5, 1.0))
        val ghost = PairCachingGhostObject()
        ghost.collisionShape = ghostShape
        val ghostTransform = Transform()
        ghostTransform.setIdentity()
        ghostTransform.setTranslation(0.0, 3.0, 0.0) // floating trigger region
        ghost.setWorldTransform(ghostTransform)
        ghost.collisionFlags = CollisionFlags.NO_CONTACT_RESPONSE // no physics response

        // Must register ghost object in collision world
        world.addCollisionObject(ghost, CollisionFilterGroups.SENSOR_TRIGGER, (-1).toShort())

        // Falling dynamic sphere
        val sphereShape: CollisionShape = SphereShape(0.25)
        val sphere: RigidBody = StackOfBoxesTest.Companion.createRigidBody(1f, Vector3d(0f, 5f, 0f), sphereShape)
        world.addRigidBody(sphere)

        var entered = false
        var exited = false
        var wasInside = false

        // Simulate
        val timeStep = 1f / 60f
        val steps = 70

        repeat(steps) {
            world.stepSimulation(timeStep.toDouble())

            var inside = false

            val overlappingPairs = ghost.overlappingPairs
            for (j in overlappingPairs.indices) {
                if (overlappingPairs[j] === sphere) {
                    inside = true
                    break
                }
            }

            if (inside && !wasInside) {
                entered = true
            } else if (!inside && wasInside) {
                exited = true
            }

            wasInside = inside
        }

        // Assertions
        Assertions.assertTrue(entered, "Sphere should have entered ghost zone.")
        Assertions.assertTrue(exited, "Sphere should have exited ghost zone.")
    }
}