package com.bulletphysics

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.linearmath.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.joml.Vector3d

class DominoChainTest {
    @Test
    fun testDominoTopple() {
        runDominoToppleTest(3)
        runDominoToppleTest(20)
        runDominoToppleTest(100)
    }

    private fun runDominoToppleTest(dominoCount: Int) {
        val world: DiscreteDynamicsWorld = StackOfBoxesTest.Companion.createWorld()
        StackOfBoxesTest.Companion.createGround(world)

        // Domino dimensions (in meters)
        val scaleForStability = 2f
        val height = 0.05f * scaleForStability
        val width = 0.025f * scaleForStability
        val depth = 0.01f * scaleForStability
        val dominoShape: CollisionShape =
            BoxShape(Vector3d((width / 2).toDouble(), (height / 2).toDouble(), (depth / 2).toDouble()))

        // Positioning
        val spacing = depth + 0.002f // slight gap
        val startX = 0f
        val y = height * 0.85f
        val z = 0f

        val dominos = Array(dominoCount) {
            val domino = StackOfBoxesTest.createRigidBody(0.05f, Vector3d(startX + it * spacing, y, z), dominoShape)
            domino.friction = 0.5
            domino.restitution = 0.1
            world.addRigidBody(domino)
            domino
        }

        // Push first domino with angular impulse to tip it forward
        val firstDomino = dominos[0]
        // firstDomino.applyTorqueImpulse(new Vector3d(0, 0, -0.02f)); // tip around X-axis (forward)
        firstDomino.applyImpulse(
            Vector3d(0.02, 0.0, 0.0),
            Vector3d(0.0, (height / 2).toDouble(), (depth / 2).toDouble())
        )

        var lastFallenCount = 0
        val timeStep = 1f / 240f
        val numSteps = 2000
        for (i in 0 until numSteps) {
            world.stepSimulation(timeStep.toDouble(), 10)

            var fallen = 0
            for (d in 0 until dominoCount) {
                val tf = Transform()
                dominos[d].motionState!!.getWorldTransform(tf)
                val up = Vector3d()
                tf.basis.getColumn(1, up) // local Y axis
                val dot = up.dot(Vector3d(0.0, 1.0, 0.0)) // how aligned with world up
                if (dot < 0.7f) { // ~45 degrees tipped
                    fallen++
                }
            }

            if (fallen != lastFallenCount) {
                System.out.printf("[$dominoCount] Step %d: %d fallen%n", i, fallen)
                lastFallenCount = fallen
            }
        }

        // Check that the last domino has fallen (tipped significantly)
        val lastTransform = Transform()
        dominos[dominoCount - 1].motionState!!.getWorldTransform(lastTransform)

        val upVector = Vector3d()
        lastTransform.basis.getColumn(1, upVector) // Y-axis of the last domino

        val verticalDot = upVector.dot(Vector3d(0.0, 1.0, 0.0)) // close to 1 if upright
        val isFallen = verticalDot < 0.7f // less than ~45° from upright

        Assertions.assertTrue(
            isFallen,
            "Domino chain failed for N=$dominoCount. Final up vector dot: $verticalDot"
        )
    }
}