package me.anno.tests.physics

import me.anno.box2d.Box2dPhysics
import me.anno.box2d.CircleCollider
import me.anno.box2d.DynamicBody2d
import me.anno.box2d.RectCollider
import me.anno.box2d.StaticBody2d
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityUtils.setContains
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.TAUf
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.cos
import kotlin.math.sin

class Box2dTest {

    val physics get() = Box2dPhysics

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testGravity() {

        val gravity = 1f
        setupGravityTest(gravity)

        val rigidbody = DynamicBody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(CircleCollider())

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1f
        var expectedPosition = 0f
        var expectedVelocity = 0f
        repeat(20) {

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
    @Execution(ExecutionMode.SAME_THREAD)
    fun testApplyForceAtCenter() {

        val gravity = 0f
        setupGravityTest(gravity)

        val rigidbody = DynamicBody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(CircleCollider())
            .add(object : Component(), OnPhysicsUpdate {
                override fun onPhysicsUpdate(dt: Double) {
                    val mass = 1f * PIf
                    rigidbody.applyForce(Vector2f(1f * mass, 2f * mass))
                }
            })

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1.0
        val expectedPosition = Vector3d()
        val expectedVelocity = Vector2f()
        repeat(20) {

            val actualPosition = sphere.position
            val actualVelocity = rigidbody.linearVelocity
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            expectedVelocity.add(1f, 2f)
            expectedPosition.add(expectedVelocity.x * dt, expectedVelocity.y * dt, 0.0)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testApplyForceNonCenter() {

        val gravity = 0f
        setupGravityTest(gravity)

        val speed = 0.02f
        val rigidbody = DynamicBody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(CircleCollider())
            .add(object : Component(), OnPhysicsUpdate {
                override fun onPhysicsUpdate(dt: Double) {
                    val mass = speed * PIf
                    for (i in 0 until 4) {
                        val angle = i * TAUf * 0.25f
                        rigidbody.applyForce(
                            Vector2f(0f, mass).rotate(angle),
                            Vector2f(mass, 0f).rotate(angle)
                        )
                    }
                }
            })

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1.0
        var expectedRotation = 0f
        repeat(20) {

            val actualPosition = sphere.position
            val actualRotation = sphere.rotation.getEulerAngleYXZvZ()
            val actualVelocity = rigidbody.linearVelocity
            // todo why is this 0.0 below a certain threshold force???
            val actualRotationVel = rigidbody.angularVelocity
            assertEquals(Vector3d(), actualPosition, 1e-6)
            assertEquals(Vector2f(), actualVelocity, 1e-6)
            assertEquals(expectedRotation, actualRotation, 0.002f)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            expectedRotation += 0.5f * speed
        }
    }

    // todo test apply impulse

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testDisabledStates() {

        Systems.registerSystem(physics)

        val gravity = 1f
        setupGravityTest(gravity)

        val rigidbody = DynamicBody2d()
        val collider = CircleCollider()
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
    @Execution(ExecutionMode.SAME_THREAD)
    fun testExternalForces() {
        val gravity = -1f
        setupGravityTest(gravity)

        val extraAcceleration = 2.5f

        class ExtraForceComponent : Component(), OnPhysicsUpdate {
            override fun onPhysicsUpdate(dt: Double) {
                val rb = getComponent(DynamicBody2d::class)!!
                val circle = getComponent(CircleCollider::class)!!
                val mass = circle.radius * circle.radius * PIf * circle.density
                rb.applyForce(0f, extraAcceleration * mass)
            }
        }

        val rigidbody = DynamicBody2d()
        val sphere = Entity()
            .add(rigidbody)
            .add(ExtraForceComponent())
            .add(CircleCollider())

        val world = Entity().add(sphere)
        Systems.world = world

        val dt = 1f
        var expectedPosition = 0f
        var expectedVelocity = 0f
        repeat(20) {

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
    @Execution(ExecutionMode.SAME_THREAD)
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

        val scene = Entity()
        Entity("Floor", scene)
            .setPosition(0.0, -10.0, 0.0)
            .add(StaticBody2d())
            .add(RectCollider().apply {
                friction = floorFriction
                halfExtents.set(30f, 10f)
            })

        val underTest = DynamicBody2d()
        underTest.angularVelocity = 1f
        val sphere = Entity()
            .setPosition(0.0, 1.0 + 1.0 / 8.0, 0.0)
            .add(underTest)
            .add(CircleCollider().apply {
                friction = circleFriction
            })
        scene.add(sphere)

        Systems.world = scene

        assertEquals(Vector3d(0.0, 1.125, 0.0), sphere.position)
        assertEquals(Vector2f(0f, 0f), underTest.linearVelocity)
        assertEquals(1f, underTest.angularVelocity)

        val dt = 1f / 8f
        physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

        // sphere falls down completely
        assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.01)
        assertEquals(Vector2f(0f, 0f), underTest.linearVelocity)
        assertEquals(1f, underTest.angularVelocity)

        repeat(3) {
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
    @Execution(ExecutionMode.SAME_THREAD)
    fun testStartRollingOnDecline() {

        val angle = 0.1f

        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0f, 0f, angle)
            .add(DynamicBody2d())
            .add(RectCollider().apply {
                friction = 0.5f
                halfExtents.set(30f, 10f)
            })
        world.add(floor)

        val underTest = DynamicBody2d()
        val dist = 10f + 1.0
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .add(underTest)
            .add(CircleCollider().apply {
                friction = 0.5f
            })
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        repeat(8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        assertEquals(Vector2f(-0.66f, -0.066f), underTest.linearVelocity, 0.01)
        assertEquals(0.666f, underTest.angularVelocity, 0.001f)
    }

    /**
     * test that bodies start sliding on declines with friction starting at a certain angle
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testStartSlidingOnDecline() {

        // todo it looks like this test moves with all given angles... how???
        val angle = 0.5f

        val gravity = -10f
        setupGravityTest(gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0f, 0f, angle)
            .add(DynamicBody2d())
            .add(RectCollider().apply {
                friction = 0.9f
                halfExtents.set(30f, 10f)
            })
        world.add(floor)

        val underTest = DynamicBody2d()
        val dist = 10f + 1.0
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .setRotation(0f, 0f, angle)
            .add(underTest)
            .add(RectCollider().apply {
                halfExtents.set(1f)
                friction = 0.9f
            })
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        repeat(8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            println("${sphere.position} += ${underTest.linearVelocity}/${underTest.angularVelocity}")
        }
    }

    // todo test interaction between all shapes
    //  - no sliding gravity; same mass, crash on one shall stop it and give the other equal velocity
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testTransferringImpulse() {

        setupGravityTest(0f)

        val world = Entity()

        fun createSphere(x: Double, dx: Double): Pair<Entity, DynamicBody2d> {
            val body = DynamicBody2d()
            body.linearVelocity.x = dx.toFloat()
            val sphere = Entity()
                .setPosition(x, 1.0, 0.0)
                .add(body)
                .add(CircleCollider().apply {
                    friction = 0f
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
        repeat(20) {
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