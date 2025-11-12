package com.bulletphysics

import com.bulletphysics.StackOfBoxesTest.Companion.createGround
import com.bulletphysics.StackOfBoxesTest.Companion.createRigidBody
import com.bulletphysics.StackOfBoxesTest.Companion.createWorld
import me.anno.ecs.components.collider.CollisionFilters.ALL_MASK
import me.anno.ecs.components.collider.CollisionFilters.GHOST_GROUP_ID
import me.anno.ecs.components.collider.CollisionFilters.createFilter
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.GhostObject
import com.bulletphysics.collision.dispatch.GhostPairCallback
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.SphereShape
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class GhostObjectTest {
    @Test
    fun testGhostObjectOverlapDetection() {
        val world = createWorld()
        world.broadphase.overlappingPairCache.setInternalGhostPairCallback(GhostPairCallback())
        createGround(world)

        // Ghost object region (box shape)
        val ghostShape = BoxShape(Vector3f(1.0, 0.5, 1.0))
        val ghost = GhostObject()
        ghost.collisionShape = ghostShape
        val ghostTransform = ghost.worldTransform
        ghostTransform.setIdentity()
        ghostTransform.setTranslation(0.0, 3.0, 0.0) // floating trigger region
        ghost.collisionFlags = CollisionFlags.NO_CONTACT_RESPONSE // no physics response

        // Must register ghost object in collision world
        world.addCollisionObject(ghost, createFilter(GHOST_GROUP_ID, ALL_MASK))

        // Falling dynamic sphere
        val sphereShape = SphereShape(0.25f)
        val sphere = createRigidBody(1f, Vector3d(0f, 5f, 0f), sphereShape)
        world.addRigidBody(sphere)

        var entered = false
        var exited = false
        var wasInside = false

        // Simulate
        val timeStep = 1f / 60f
        val steps = 70

        repeat(steps) {
            world.stepSimulation(timeStep)

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
        assertTrue(entered, "Sphere should have entered ghost zone.")
        assertTrue(exited, "Sphere should have exited ghost zone.")
    }
}