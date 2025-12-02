package com.bulletphysics

import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint
import com.bulletphysics.dynamics.constraintsolver.SliderConstraint
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.constraints.GenericConstraint
import me.anno.bullet.constraints.PointConstraint
import me.anno.maths.Maths.PIf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ConstraintTest {
    @Test
    fun testPoint2PointConstraint() {
        val world = StackOfBoxesTest.createWorld()

        // Two bodies
        val bodyA = createDynamicBox(Vector3d(-1f, 5f, 0f), 1f)
        val bodyB = createDynamicBox(Vector3d(1f, 5f, 0f), 1f)
        world.addRigidBody(bodyA)
        world.addRigidBody(bodyB)

        // Link via ball joint
        val settings = PointConstraint()
        val p2p = Point2PointConstraint(
            settings, bodyA, bodyB,
            Vector3d(0.5, 0.0, 0.0), Vector3d(-0.5, 0.0, 0.0)
        )
        world.addConstraint(p2p, true)

        simulate(world, 240)

        val delta = Vector3d()
        bodyA.worldTransform.origin.sub(bodyB.worldTransform.origin, delta)
        val dist = delta.length()
        println("P2P distance: $dist")
        Assertions.assertTrue(dist < 1.2f, "Bodies should remain close due to point2point constraint")
    }

    @Test
    fun testHingeConstraint() {
        val world = StackOfBoxesTest.createWorld()

        val base = createStaticBox(Vector3d(0f, 5f, 0f))
        val bar = createDynamicBox(Vector3d(0.1f, 4f, 0f), 1f)
        world.addRigidBody(base)
        world.addRigidBody(bar)

        val pivotInA = Transform()
        pivotInA.setIdentity()
        pivotInA.setTranslation(0.0, -0.5, 0.0)
        val pivotInB = Transform()
        pivotInB.setIdentity()
        pivotInB.setTranslation(0.0, 0.5, 0.0)

        val settings = me.anno.bullet.constraints.HingeConstraint()
        val hinge = HingeConstraint(settings, base, bar, pivotInA, pivotInB)
        world.addConstraint(hinge, true)

        simulate(world, 240)

        val angle = hinge.hingeAngle
        println("Hinge angle: $angle")
        Assertions.assertTrue(abs(angle) > 0.01f, "Hinge should allow rotation")
    }

    @Test
    fun testSliderConstraint() {
        val world = StackOfBoxesTest.createWorld()

        val base = createStaticBox(Vector3d(0f, 0f, 0f))
        val slider = createDynamicBox(Vector3d(0f, 0f, 0.5f), 1f)
        world.addRigidBody(base)
        world.addRigidBody(slider)

        val frameInA = Transform()
        frameInA.setIdentity()
        frameInA.setTranslation(0.0, 0.0, 0.0)
        val frameInB = Transform()
        frameInB.setIdentity()
        frameInB.setTranslation(0.0, 0.0, 0.0)

        val settings = me.anno.bullet.constraints.SliderConstraint()
        val sliderConstraint = SliderConstraint(settings, base, slider, frameInA, frameInB, true)
        world.addConstraint(sliderConstraint, true)

        slider.activationState = ActivationState.ALWAYS_ACTIVE
        slider.applyCentralForce(Vector3f(20.0, 20.0, 20.0)) // Slide forward

        simulate(world, 240)

        val pos = slider.worldTransform.origin
        Assertions.assertTrue(abs(pos.y) < 1e-9)
        Assertions.assertTrue(abs(pos.z) < 1e-9)
        Assertions.assertTrue(pos.x > 0.5f, "Slider should move along Z axis")
    }

    @Test
    fun testGeneric6DofConstraint() {
        val world = StackOfBoxesTest.createWorld()

        val base = createStaticBox(Vector3d(0f, 0f, 0f))
        val body = createDynamicBox(Vector3d(0f, 0f, 0.5f), 1f)
        world.addRigidBody(base)
        world.addRigidBody(body)

        val frameInA = Transform()
        val frameInB = Transform()

        val settings = GenericConstraint()
        val dof = Generic6DofConstraint(settings, base, body, frameInA, frameInB, true)
        dof.linearLimits.lowerLimit.set(-1.0, 0.0, 0.0)
        dof.linearLimits.upperLimit.set(1.0, 0.0, 0.0) // Only X axis movement allowed
        world.addConstraint(dof, true)

        body.activationState = ActivationState.ALWAYS_ACTIVE
        body.applyCentralForce(Vector3f(20.0, 0.0, 20.0))

        simulate(world, 240)

        val pos = body.worldTransform.origin
        println("6DoF position: $pos")
        Assertions.assertTrue(abs(pos.z) < 0.1f, "Movement in Z should be restricted")
        Assertions.assertTrue(pos.x > 0.5f, "Movement in X should be allowed")
    }

    @Test
    fun testConstraintBreaking() {
        val world = StackOfBoxesTest.createWorld()

        // Create two dynamic bodies
        val bodyA = createDynamicBox(Vector3d(0f, 5f, 0f), 1f)
        val bodyB = createDynamicBox(Vector3d(0f, 4f, 0f), 1f)
        world.addRigidBody(bodyA)
        world.addRigidBody(bodyB)

        // Attach with a point2point (ball-socket) constraint
        val settings = PointConstraint()
        val constraint = Point2PointConstraint(
            settings, bodyA, bodyB,
            Vector3d(0.0, -0.5, 0.0), Vector3d(0.0, 0.5, 0.0)
        )
        constraint.breakingImpulse = 5.0f // Very low threshold
        world.addConstraint(constraint, true)

        // Apply strong impulse to break it
        bodyB.applyCentralImpulse(Vector3f(50.0, 0.0, 0.0))

        simulate(world, 60)

        // Check whether constraint has been removed
        val broken = world.constraints.isEmpty()
        println("Constraint broken: $broken")

        Assertions.assertTrue(broken, "The constraint should break under strong impulse.")
        Assertions.assertTrue(constraint.isBroken)
    }

    @Test
    fun testHingeMotorWithLimit() {
        val world = StackOfBoxesTest.createWorld()

        // Static anchor and rotating bar
        val base = createStaticBox(Vector3d(0f, 5f, 0f))
        val bar = createDynamicBox(Vector3d(1f, 5f, 0f), 1f)
        world.addRigidBody(base)
        world.addRigidBody(bar)

        val pivotInA = Transform()
        pivotInA.setIdentity()
        pivotInA.setTranslation(0.5, 0.0, 0.0)

        val pivotInB = Transform()
        pivotInB.setIdentity()
        pivotInB.setTranslation(-0.5, 0.0, 0.0)

        val settings = me.anno.bullet.constraints.HingeConstraint()
        val hinge = HingeConstraint(settings, base, bar, pivotInA, pivotInB)
        settings.lowerLimit = -PIf / 4f // ±45°
        settings.upperLimit = PIf / 4f
        settings.limitSoftness = 0.9f
        settings.biasFactor = 0.3f
        settings.relaxation = 1.0f
        settings.motorVelocity = 2.0f
        hinge.maxMotorImpulse = 0.1f

        world.addConstraint(hinge, true)

        println("Limit: " + Math.PI / 4)
        repeat(10) {
            println("hinge angle: " + hinge.hingeAngle)
            simulate(world, 18)
        }

        val angle = hinge.hingeAngle
        println("Final hinge angle: $angle")

        Assertions.assertTrue(abs(angle) <= Math.PI / 4 + 0.05f, "Hinge angle should not exceed limit.")
    }

    private fun createDynamicBox(pos: Vector3d, mass: Float): RigidBody {
        val shape = BoxShape(Vector3f(0.5, 0.5, 0.5))
        val inertia = Vector3f()
        shape.calculateLocalInertia(mass, inertia)
        val body = RigidBody(mass, shape, inertia)
        val tmp = body.worldTransform
        tmp.setIdentity()
        tmp.setTranslation(pos)
        body.setInitialTransform(tmp)
        return body
    }

    private fun createStaticBox(pos: Vector3d): RigidBody {
        return createDynamicBox(pos, 0f)
    }

    private fun simulate(world: DynamicsWorld, steps: Int) {
        val timeStep = 1f / 60f
        repeat(steps) {
            world.stepSimulation(timeStep)
        }
    }
}
