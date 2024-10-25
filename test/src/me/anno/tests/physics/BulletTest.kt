package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityUtils.setContains
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class BulletTest {

    object BulletPhysicsImpl : BulletPhysics()

    val physics = BulletPhysicsImpl

    init {
        LogManager.define("BulletPhysics", Level.DEBUG)
        physics.printValidations = true
    }

    @Test
    fun testGravity() {

        val gravity = 1f
        setupGravityTest(gravity)

        val rigidbody = Rigidbody()
        val sphere = Entity()
            .add(rigidbody.apply {
                linearDamping = 0.0
                mass = 1.0
            })
            .add(SphereCollider())

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1.0
        var expectedPosition = 0.0
        var expectedVelocity = 0.0
        for (i in 0 until 200) {

            val actualPosition = sphere.position.y
            val actualVelocity = rigidbody.linearVelocity.y
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            expectedVelocity += gravity * dt
            expectedPosition += expectedVelocity * dt //  + (gravity * dt² * 0.5)
        }
    }

    @Test
    fun testDisabledStates() {

        Systems.registerSystem(physics)

        val gravity = 1f
        setupGravityTest(gravity)

        val rigidbody = Rigidbody()
        val collider = SphereCollider()
        val sphere = Entity()
            .add(rigidbody.apply {
                linearDamping = 0.0
                mass = 1.0
            })
            .add(collider)

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1.0
        var expectedPosition = 0.0
        var expectedVelocity = 0.0
        val numFlags = 7
        val testAllCombinations = false
        val numTests = if (testAllCombinations) (1 shl (numFlags - 1)) * 8 + 4 else numFlags * 8 + 4
        for (i in 0 until numTests) {

            // if any flag of this is true, the rigidbody is invalid or static, and must not be moved
            val di = i / 4
            val disabledMask = if (di.hasFlag(1)) {
                if (testAllCombinations) di.shr(1)
                else (1 shl di.shr(1))
            } else 0
            sphere.isEnabled = !disabledMask.hasFlag(1)
            rigidbody.isEnabled = !disabledMask.hasFlag(2)
            collider.isEnabled = !disabledMask.hasFlag(4)
            rigidbody.mass = if (disabledMask.hasFlag(8)) 0.0 else 1.0
            val isPartOfWorld = !disabledMask.hasFlag(16)
            val hasRigidbody = !disabledMask.hasFlag(32)
            val hasCollider = !disabledMask.hasFlag(64)
            world.setContains(sphere, isPartOfWorld)
            sphere.setContains(rigidbody, hasRigidbody)
            sphere.setContains(collider, hasCollider)

            val actualPosition = sphere.position.y
            val actualVelocity = rigidbody.linearVelocity.y
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            if (disabledMask == 0) {
                expectedVelocity += gravity * dt
                expectedPosition += expectedVelocity * dt //  + (gravity * dt² * 0.5)
            }
        }
    }

    private fun setupGravityTest(gravity: Float) {
        Systems.registerSystem(physics)

        physics.gravity = Vector3d(0.0, gravity.toDouble(), 0.0)
        physics.allowedSpace.all()
        physics.clear()

        physics.fixedStep = 0.0
        physics.maxSubSteps = 0
    }

    /**
     * fly using upwards force (thrusters) against gravity
     * */
    @Test
    fun testExternalForces() {
        val gravity = -1f
        setupGravityTest(gravity)

        val extraAcceleration = 2.5f

        class ExtraForceComponent : Component(), OnPhysicsUpdate {
            override fun onPhysicsUpdate(dt: Double) {
                val rb = getComponent(Rigidbody::class)!!
                val mass = rb.mass
                rb.applyForce(0.0, extraAcceleration * mass, 0.0)
            }
        }

        val rigidbody = Rigidbody()
        val sphere = Entity()
            .add(rigidbody.apply {
                linearDamping = 0.0
                mass = 1.0
            })
            .add(ExtraForceComponent())
            .add(SphereCollider())

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1.0
        var expectedPosition = 0.0
        var expectedVelocity = 0.0
        for (i in 0 until 20) {

            val actualPosition = sphere.position.y
            val actualVelocity = rigidbody.linearVelocity.y
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            expectedVelocity += (gravity + extraAcceleration) * dt
            expectedPosition += expectedVelocity * dt // + gravity * 0.5 * dt²
        }
    }

    @Test
    fun testSlidingToRollingConversion() {
        testRotatingToRolling(1f, 1f)
        testRotatingToRolling(1f, 0f)
        testRotatingToRolling(0f, 1f)
        testRotatingToRolling(0f, 0f)
    }

    /**
     * test that bodies start rolling when they are rotating and fall on a surface with friction
     * */
    fun testRotatingToRolling(floorFriction: Float, circleFriction: Float) {
        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setPosition(0.0, -10.0, 0.0)
            .add(Rigidbody().apply {
                friction = floorFriction.toDouble()
            })
            .add(BoxCollider().apply {
                halfExtends.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = Rigidbody()
        underTest.angularVelocity.z = 1.0
        val sphere = Entity()
            .setPosition(0.0, 1.0 + 1.0 / 8.0, 0.0)
            .add(underTest.apply {
                friction = circleFriction.toDouble()
                linearDamping = 0.0
                angularDamping = 0.0
                mass = 1.0
            })
            .add(SphereCollider())
        world.add(sphere)

        Systems.world = world

        assertEquals(Vector3d(0.0, 1.125, 0.0), sphere.position)
        assertEquals(Vector3d(0.0), underTest.linearVelocity)
        assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.angularVelocity)

        val dt = 1f / 8f
        physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

        // sphere falls down completely
        assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.05)
        // assertEquals(Vector3d(0.0, 0.0, 0.0), underTest.velocity)
        // assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.angularVelocity)

        for (i in 0 until 3) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        if (floorFriction > 0f && circleFriction > 0f) {
            assertTrue(sphere.position.x < 0f)
            assertEquals(-0.365, underTest.linearVelocity.x, 0.001)
            assertEquals(0.0, underTest.linearVelocity.y, 0.05) // todo why is there y-movement??
            assertEquals(Vector3d(0.0, 0.0, 0.365), underTest.angularVelocity, 0.01)
        } else {
            assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.05)
            assertEquals(Vector3d(0.0, 0.03, 0.0), underTest.linearVelocity, 0.01) // todo why is there y-movement??
            assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.angularVelocity, 0.06)
        }
    }

    /**
     * test that bodies start rolling on declines with friction starting at a certain angle
     * why is any angle sufficient to start rolling?? because starting rolling takes very little effort
     * */
    @Test
    fun testStartRollingOnDecline() {

        val angle = 0.1

        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0.0, 0.0, angle)
            .add(Rigidbody().apply {
                friction = 0.5
            })
            .add(BoxCollider().apply {
                halfExtends.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = Rigidbody()
        val dist = 10f + 1f
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .add(underTest.apply {
                linearDamping = 0.0
                angularDamping = 0.0
                friction = 0.5
                mass = 1.0
            })
            .add(SphereCollider())
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        for (i in 0 until 8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        assertEquals(Vector3d(-0.848, -0.08, 0.0), underTest.linearVelocity, 0.01)
        assertEquals(Vector3d(0.0, 0.0, 0.855), underTest.angularVelocity, 0.01)
    }

    /**
     * test that bodies start sliding on declines with friction starting at a certain angle
     * */
    @Test
    fun testStartSlidingOnDecline() {

        val angle = 0.5

        // todo does it move on all angles??
        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0.0, 0.0, angle)
            .add(Rigidbody().apply {
                friction = 0.9
            })
            .add(BoxCollider().apply {
                halfExtends.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = Rigidbody()
        val dist = 10f + 1f
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .setRotation(0.0, 0.0, angle)
            .add(underTest.apply {
                friction = 0.9
                mass = 1.0
            })
            .add(BoxCollider().apply {
                halfExtends.set(1.0)
            })
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        for (i in 0 until 8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            println("${sphere.position} += ${underTest.linearVelocity}/${underTest.angularVelocity}")
        }
    }

    // todo test interaction between all shapes
    //  - no sliding gravity; same mass, crash on one shall stop it and give the other equal velocity
    @Test
    fun testTransferringImpulse() {

        setupGravityTest(0f)

        val world = Entity()

        fun createSphere(x: Double, dx: Double): Pair<Entity, Rigidbody> {
            val body = Rigidbody()
            body.linearVelocity.x = dx
            val sphere = Entity()
                .setPosition(x, 1.0, 0.0)
                .add(body.apply {
                    mass = 1.0
                    friction = 0.0
                })
                .add(SphereCollider())
            world.add(sphere)
            return (sphere to body)
        }

        val (s0, b0) = createSphere(-2.5, 1.0)
        val (s1, b1) = createSphere(0.0, 0.0)

        Systems.world = world

        val dt = 1f / 8f
        for (i in 0 until 20) {
            println("${s0.position.x} += ${b0.linearVelocity.x} | ${s1.position.x} += ${b1.linearVelocity.x}")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
    }

    // what happens when one collider has a !=0-density, and another has a =0-density within the same rigidbody?
    // iff any shape has mass, the body is dynamic

}