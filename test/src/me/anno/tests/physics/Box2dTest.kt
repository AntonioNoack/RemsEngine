package me.anno.tests.physics

import me.anno.box2d.Box2dPhysics
import me.anno.box2d.CircleCollider
import me.anno.box2d.RectCollider
import me.anno.box2d.Rigidbody2d
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityUtils.setContains
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class Box2dTest {

    val physics get() = Box2dPhysics

    @Test
    fun testGravity() {

        val gravity = 1f
        setupGravityTest(gravity)

        val rigidbody = Rigidbody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(CircleCollider().apply { density = 1f })

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1f
        var expectedPosition = 0f
        var expectedVelocity = 0f
        for (i in 0 until 20) {

            val actualPosition = sphere.position.y.toFloat()
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

        val rigidbody = Rigidbody2d()
        val collider = CircleCollider()
        collider.density = 1f
        val sphere = Entity()
            .add(rigidbody)
            .add(collider)

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1f
        var expectedPosition = 0f
        var expectedVelocity = 0f
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
            collider.density = if (disabledMask.hasFlag(8)) 0f else 1f
            val isPartOfWorld = !disabledMask.hasFlag(16)
            val hasRigidbody = !disabledMask.hasFlag(32)
            val hasCollider = !disabledMask.hasFlag(64)
            world.setContains(sphere, isPartOfWorld)
            sphere.setContains(rigidbody, hasRigidbody)
            sphere.setContains(collider, hasCollider)

            val actualPosition = sphere.position.y.toFloat()
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
        physics.positionIterations = 1
        physics.velocityIterations = 1
        physics.allowedSpace.all()
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
                val rb = getComponent(Rigidbody2d::class)!!
                val circle = getComponent(CircleCollider::class)!!
                val mass = circle.radius * circle.radius * PIf * circle.density
                rb.applyForce(0f, extraAcceleration * mass)
            }
        }

        val rigidbody = Rigidbody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(ExtraForceComponent())
            .add(CircleCollider().apply { density = 1f })

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1f
        var expectedPosition = 0f
        var expectedVelocity = 0f
        for (i in 0 until 20) {

            val actualPosition = sphere.position.y.toFloat()
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
            .add(Rigidbody2d())
            .add(RectCollider().apply {
                friction = floorFriction
                halfExtends.set(30f, 10f)
            })
        world.add(floor)

        val underTest = Rigidbody2d()
        underTest.angularVelocity = 1f
        val sphere = Entity()
            .setPosition(0.0, 1.0 + 1.0 / 8.0, 0.0)
            .add(underTest)
            .add(CircleCollider().apply {
                friction = circleFriction
                density = 1f
            })
        world.add(sphere)

        Systems.world = world

        assertEquals(Vector3d(0.0, 1.125, 0.0), sphere.position)
        assertEquals(Vector2f(0f, 0f), underTest.linearVelocity)
        assertEquals(1f, underTest.angularVelocity)

        val dt = 1f / 8f
        physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

        // sphere falls down completely
        assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.01)
        assertEquals(Vector2f(0f, 0f), underTest.linearVelocity)
        assertEquals(1f, underTest.angularVelocity)

        for (i in 0 until 3) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        if (floorFriction > 0f && circleFriction > 0f) {
            assertTrue(sphere.position.x < 0f)
            assertEquals(-0.333f, underTest.linearVelocity.x, 0.001f)
            assertEquals(0f, underTest.linearVelocity.y)
            assertEquals(0.333f, underTest.angularVelocity, 0.002f)
        } else {
            assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.01)
            assertEquals(Vector2f(0f, 0f), underTest.linearVelocity)
            assertEquals(1f, underTest.angularVelocity)
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
            .add(Rigidbody2d())
            .add(RectCollider().apply {
                friction = 0.5f
                halfExtends.set(30f, 10f)
            })
        world.add(floor)

        val underTest = Rigidbody2d()
        val dist = 10f + 1f
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .add(underTest)
            .add(CircleCollider().apply {
                friction = 0.5f
                density = 1f
            })
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        for (i in 0 until 8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        assertEquals(Vector2f(-0.66f, -0.066f), underTest.linearVelocity, 0.01)
        assertEquals(0.666f, underTest.angularVelocity, 0.001f)
    }

    /**
     * test that bodies start sliding on declines with friction starting at a certain angle
     * */
    @Test
    fun testStartSlidingOnDecline() {

        // todo it looks like this test moves with all given angles... how???
        val angle = 0.5

        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0.0, 0.0, angle)
            .add(Rigidbody2d())
            .add(RectCollider().apply {
                friction = 0.9f
                halfExtends.set(30f, 10f)
            })
        world.add(floor)

        val underTest = Rigidbody2d()
        val dist = 10f + 1f
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .setRotation(0.0, 0.0, angle)
            .add(underTest)
            .add(RectCollider().apply {
                halfExtends.set(1f)
                friction = 0.9f
                density = 1f
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

        fun createSphere(x: Double, dx: Double): Pair<Entity, Rigidbody2d> {
            val body = Rigidbody2d()
            body.linearVelocity.x = dx.toFloat()
            val sphere = Entity()
                .setPosition(x, 1.0, 0.0)
                .add(body)
                .add(CircleCollider().apply {
                    friction = 0f
                    density = 1f
                })
            world.add(sphere)
            return (sphere to body)
        }

        val (s0, b0) = createSphere(-2.5, 1.0)
        val (s1, b1) = createSphere(0.0, 0.0)

        Systems.world = world

        // todo we'd expect the impulse to be transferred perfectly from s0 to s1,
        //  but instead, they stick together

        val dt = 1f / 8f
        for (i in 0 until 20) {
            println("${s0.position.x} += ${b0.linearVelocity.x} | ${s1.position.x} += ${b1.linearVelocity.x}")
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        assertTrue(s0.position.x > -2.5)
        assertTrue(s1.position.x > 0.0)
        assertTrue(b0.linearVelocity.x >= 0.0)
        assertTrue(b1.linearVelocity.x > 0.0)
        assertEquals(1f, b0.linearVelocity.x + b1.linearVelocity.x)

    }

    // what happens when one collider has a !=0-density, and another has a =0-density within the same rigidbody?
    // iff any shape has mass, the body is dynamic

}