package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityUtils.setContains
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.Optimization.simplexAlgorithm
import me.anno.tests.LOGGER
import me.anno.tests.physics.constraints.createBridgeMeshes
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.f3s
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.sin
import kotlin.math.sqrt

class BulletTest {

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testGravity() {

        val gravity = 1f
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val dynamicBody = DynamicBody()
        val sphere = Entity()
            .add(dynamicBody.apply {
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
            val actualVelocity = dynamicBody.globalLinearVelocity.y
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            expectedVelocity += gravity * dt
            expectedPosition += expectedVelocity * dt //  + (gravity * dt² * 0.5)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testDisabledStates() {

        val gravity = 1f
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val dynamicBody = DynamicBody()
        val collider = SphereCollider()
        val sphere = Entity()
            .add(dynamicBody.apply {
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
            dynamicBody.isEnabled = !disabledMask.hasFlag(2)
            collider.isEnabled = !disabledMask.hasFlag(4)
            dynamicBody.mass = if (disabledMask.hasFlag(8)) 0.0 else 1.0
            val isPartOfWorld = !disabledMask.hasFlag(16)
            val hasRigidbody = !disabledMask.hasFlag(32)
            val hasCollider = !disabledMask.hasFlag(64)
            world.setContains(sphere, isPartOfWorld)
            sphere.setContains(dynamicBody, hasRigidbody)
            sphere.setContains(collider, hasCollider)

            val actualPosition = sphere.position.y
            val actualVelocity = dynamicBody.globalLinearVelocity.y
            assertEquals(expectedPosition, actualPosition)
            assertEquals(expectedVelocity, actualVelocity)

            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

            if (disabledMask == 0) {
                expectedVelocity += gravity * dt
                expectedPosition += expectedVelocity * dt //  + (gravity * dt² * 0.5)
            }
        }
    }

    private fun setupGravityTest(physics: BulletPhysics, gravity: Float) {
        Systems.registerSystem(physics)

        physics.gravity = Vector3d(0.0, gravity.toDouble(), 0.0)
        physics.allowedSpace.all()

        physics.fixedStep = 0.0
        physics.maxSubSteps = 0
    }

    /**
     * fly using upwards force (thrusters) against gravity
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testExternalForces() {

        val gravity = -1f
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val extraAcceleration = 2.5f

        class ExtraForceComponent : Component(), OnPhysicsUpdate {
            override fun onPhysicsUpdate(dt: Double) {
                val rb = getComponent(DynamicBody::class)!!
                val mass = rb.mass
                rb.applyForce(0.0, extraAcceleration * mass, 0.0)
            }
        }

        val dynamicBody = DynamicBody()
        val sphere = Entity()
            .add(dynamicBody.apply {
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
            val actualVelocity = dynamicBody.globalLinearVelocity.y
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
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val world = Entity()
        val floor = Entity()
            .setPosition(0.0, -10.0, 0.0)
            .add(DynamicBody().apply {
                friction = floorFriction.toDouble()
            })
            .add(BoxCollider().apply {
                halfExtents.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = DynamicBody()
        underTest.globalAngularVelocity.z = 1.0
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
        assertEquals(Vector3d(0.0), underTest.globalLinearVelocity)
        assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.globalAngularVelocity)

        val dt = 1f / 8f
        physics.step((dt * SECONDS_TO_NANOS).toLong(), false)

        // sphere falls down completely
        assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.15)
        // assertEquals(Vector3d(0.0, 0.0, 0.0), underTest.velocity)
        // assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.angularVelocity)

        for (i in 0 until 3) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }

        if (floorFriction > 0f && circleFriction > 0f) {
            assertTrue(sphere.position.x < 0f)
            assertEquals(-0.362, underTest.globalLinearVelocity.x, 0.01)
            assertEquals(0.0, underTest.globalLinearVelocity.y, 0.08) // todo why is there y-movement??
            assertEquals(Vector3d(0.0, 0.0, 0.365), underTest.globalAngularVelocity, 0.01)
        } else {
            assertEquals(Vector3d(0.0, 1.0, 0.0), sphere.position, 0.15)
            assertEquals(Vector3d(0.0, 0.03, 0.0), underTest.globalLinearVelocity, 0.05) // todo why is there y-movement??
            assertEquals(Vector3d(0.0, 0.0, 1.0), underTest.globalAngularVelocity, 0.06)
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
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0f, 0f, angle)
            .add(DynamicBody().apply {
                friction = 0.5
            })
            .add(BoxCollider().apply {
                halfExtents.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = DynamicBody()
        val dist = 10f + 1.0
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

        assertEquals(Vector3d(-0.82, -0.07, 0.0), underTest.globalLinearVelocity, 0.04)
        assertEquals(Vector3d(0.0, 0.0, 0.814), underTest.globalAngularVelocity, 0.04)
    }

    /**
     * test that bodies start sliding on declines with friction starting at a certain angle
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testStartSlidingOnDecline() {

        val angle = 0.5f

        // todo does it move on all angles??
        val gravity = -10f
        val physics = BulletPhysics()
        setupGravityTest(physics, gravity)

        val world = Entity()
        val floor = Entity()
            .setRotation(0f, 0f, angle)
            .add(DynamicBody().apply {
                friction = 0.9
            })
            .add(BoxCollider().apply {
                halfExtents.set(30.0, 10.0, 30.0)
            })
        world.add(floor)

        val underTest = DynamicBody()
        val dist = 10f + 1.0
        val sphere = Entity()
            .setPosition(-sin(angle) * dist, cos(angle) * dist, 0.0)
            .setRotation(0f, 0f, angle)
            .add(underTest.apply {
                friction = 0.9
                mass = 1.0
            })
            .add(BoxCollider().apply {
                halfExtents.set(1.0)
            })
        world.add(sphere)

        Systems.world = world

        val dt = 1f / 8f
        for (i in 0 until 8) {
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            println("${sphere.position} += ${underTest.globalLinearVelocity}/${underTest.globalAngularVelocity}")
        }
    }

    // todo test interaction between all shapes
    //  - no sliding gravity; same mass, crash on one shall stop it and give the other equal velocity

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testTransferringImpulse() {

        val physics = BulletPhysics()
        setupGravityTest(physics, 0f)

        val world = Entity()

        fun createShapes(): List<Collider> {
            return listOf(
                // todo why do only these support this crash-scenario??
                BoxCollider(),
                SphereCollider(),
            )
        }

        fun createShape(): Pair<Entity, DynamicBody> {
            val body = DynamicBody()
            val entity = Entity()
                .add(body.apply {
                    mass = 1.0
                    friction = 0.0
                    restitution = 1.0
                })
                .add(SphereCollider())
            world.add(entity)
            return (entity to body)
        }

        fun resetShape(entity: Entity, body: DynamicBody, x: Double, dx: Double, collider: Collider) {
            body.globalLinearVelocity = Vector3d(dx, 0.0, 0.0)
            entity.setPosition(x, 0.0, 0.0)
            entity.remove(entity.components[1] as Collider)
            entity.add(collider)
        }

        val (s0, b0) = createShape()
        val (s1, b1) = createShape()

        Systems.world = world

        for (shape1 in createShapes()) {

            val shape2 = shape1.clone() as Collider
            assertEquals(shape1::class, shape2::class)
            assertNotSame(shape1, shape2)

            resetShape(s0, b0, -2.5, 1.0, shape1)
            resetShape(s1, b1, 0.0, 0.0, shape2)

            println("Checking ${shape1.className} vs ${shape2.className}")

            val dt = 1f / 8f
            for (i in 0 until 10) {
                physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
            }

            // check that all impulse has been transferred perfectly
            assertEquals(Vector3d(-2.25, 0.0, 0.0), s0.position, 0.3)
            assertEquals(Vector3d(0.0), b0.globalLinearVelocity, 0.15)
            assertEquals(Vector3d(1.0, 0.0, 0.0), b1.globalLinearVelocity, 0.15)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testInteraction() {

        fun createShapes(): List<Collider> {
            return listOf(
                BoxCollider(),
                SphereCollider(),
                CapsuleCollider().apply { name = "x"; axis = Axis.X },
                CapsuleCollider().apply { name = "y"; axis = Axis.Y },
                CapsuleCollider().apply { name = "z"; axis = Axis.Z },
                CylinderCollider().apply { name = "x"; axis = Axis.X },
                CylinderCollider().apply { name = "y"; axis = Axis.Y },
                CylinderCollider().apply { name = "z"; axis = Axis.Z },
                ConeCollider().apply { name = "x"; axis = Axis.X },
                // todo y and z are incompatible??
                ConeCollider().apply { name = "y"; axis = Axis.Y },
                // ConeCollider().apply { name = "z"; axis = Axis.Z },
                ConvexCollider().apply {
                    points = floatArrayOf(
                        -1f, +1f, +1f,
                        +1f, -1f, +1f,
                        +1f, +1f, -1f,
                        +1f, -1f, -1f,
                        -1f, +1f, -1f,
                        -1f, -1f, +1f
                    )
                },
                MeshCollider(flatCube),
                MeshCollider(IcosahedronModel.createIcosphere(0)),
            )
        }

        var good = 0
        var failed = 0
        for (shape1 in createShapes()) {
            for (shape2 in createShapes()) {

                val physics = BulletPhysics()
                setupGravityTest(physics, 0f)

                val world = Entity()

                fun createShape(x: Double, dx: Double, collider: Collider): Pair<Entity, DynamicBody> {
                    val body = DynamicBody()
                    body.globalLinearVelocity = Vector3d(dx, 0.0, 0.0)
                    val entity = Entity()
                        .setPosition(x, 0.0, 0.0)
                        .setRotation(0f, 0f, 0f)
                        .add(body.apply {
                            mass = 1.0
                            friction = 0.0
                            restitution = 1.0
                        })
                        .add(collider)
                    world.add(entity)
                    return (entity to body)
                }

                val (s0, b0) = createShape(-2.5, 1.0, shape1)
                val (s1, b1) = createShape(0.0, 0.0, shape2)

                Systems.world = world

                // println("Checking ${shape1.toShortString()} vs ${shape2.toShortString()}")

                val dt = 1f / 8f
                for (i in 0 until 10) {
                    physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
                }

                try {
                    // check that all impulse has been transferred perfectly
                    assertNotEquals(Vector3d(0.0), s1.position)
                    assertNotEquals(Vector3d(0.0), b1.globalLinearVelocity)

                    assertNotEquals(Vector3d(-2.5, 0.0, 0.0), s0.position)
                    assertNotEquals(Vector3d(1.0, 0.0, 0.0), b0.globalLinearVelocity)

                    // check impulse sum
                    assertEquals(Vector3d(1.0, 0.0, 0.0), b0.globalLinearVelocity + b1.globalLinearVelocity, 0.01)
                    good++
                } catch (e: Exception) {
                    LOGGER.warn("Failed ${shape1.toShortString()} vs ${shape2.toShortString()}")
                    failed++
                }
            }
        }

        println("Good: $good/${good + failed}")
        assertEquals(0, failed)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testPointConstraintByBuildingAHangingBridge() {

        val physics = BulletPhysics()
        setupGravityTest(physics, -10f)

        val debug = false
        val n = 31
        val dist = 2.5
        val chainFactor = 0.5

        val cosh0 = cosh(0.0) // 1.0
        val cosh1 = cosh(1.0)

        // trying to find a good start, so we don't have too much bounce
        fun initialShape(i: Int): Double {
            val halfN = (n - 1) * 0.5
            val x11 = (i - halfN) / halfN
            val dy = 23.3
            return (cosh(x11) - cosh1) * dy / (cosh1 - cosh0)
        }

        val world = Entity()
        fun createPoint(i: Int, mass: Double): DynamicBody {
            val result = DynamicBody().apply {
                this.mass = mass
                linearDamping = 0.999
            }
            Entity("$i", world)
                .setPosition(i * dist, initialShape(i), 0.0)
                .add(SphereCollider())
                .add(result)
            return result
        }

        // create nodes
        val chain = ArrayList<DynamicBody>(n)
        for (i in 0 until n) {
            chain.add(createPoint(i, if (i == 0 || i == n - 1) 0.0 else 1.0))
        }

        // create links
        for (i in 1 until n) {
            val c0 = chain[i - 1].entity!!
            val c1 = chain[i]
            c0.addChild(PointConstraint().apply {
                selfPosition = selfPosition.set(-dist * chainFactor, 0.0, 0.0)
                otherPosition = otherPosition.set(+dist * chainFactor, 0.0, 0.0)
                other = c1
            })
        }

        Systems.world = world

        fun getY(i: Int): Double {
            return chain[i].transform!!.globalPosition.y
        }

        for (k in 0 until 100) {
            physics.step((0.1f * SECONDS_TO_NANOS).toLong(), false)
            if (debug) println(chain.indices.map { i -> getY(i).f3s() })
        }

        // trying to fit the chain against a theoretical chain
        fun fitShape(i: Int, y0: Double, y1: Double): Double {
            val halfN = (n - 1) * 0.5
            val x11 = (i - halfN) / halfN
            return mix(y0, y1, (cosh(x11) - cosh1) / (cosh1 - cosh0))
        }

        fun fitShapeError(y0: Double, y1: Double): Double {
            var err = 0.0
            for (i in 1 until n - 1) {
                err += sq(fitShape(i, y0, y1) - getY(i))
            }
            return err
        }

        val (bestErr, bestParams) = simplexAlgorithm(
            doubleArrayOf(0.0, 23.3), 0.3, 0.0, 100
        ) { params -> fitShapeError(params[0], params[1]) }
        val (y0, y1) = bestParams

        if (debug) println(chain.mapIndexed { i, it -> (it.transform!!.globalPosition.y - fitShape(i, y0, y1)).f3s() })
        val avgErr = sqrt(bestErr / (n - 2))

        assertEquals(3.6, y0, 1.0)
        assertEquals(30.0, y1, 2.0)
        assertTrue(avgErr < 0.7, "avgErr = $avgErr < 0.7")
        println("$bestErr -> $avgErr, $y0,$y1")
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testPointConstraintByBuildingABowBridge() {

        val physics = BulletPhysics()
        setupGravityTest(physics, -10f)

        val debug = true
        val n = 31
        val dist = 2.5
        val chainFactor = 0.51

        val cosh0 = cosh(0.0) // 1.0
        val cosh1 = cosh(1.0)

        // trying to find a good start, so we don't have too much bounce
        fun initialShape(i: Int): Double {
            val halfN = (n - 1) * 0.5
            val x11 = (i - halfN) / halfN
            val dy = 43.3
            return (cosh(x11) - cosh1) * dy / (cosh1 - cosh0)
        }

        val world = Entity()
        fun createPoint(i: Int, mass: Double): DynamicBody {
            val result = DynamicBody().apply {
                this.mass = mass
                linearDamping = 0.999
            }
            Entity("$i", world)
                .setPosition(i * dist, initialShape(i), 0.0)
                .add(SphereCollider())
                .add(result)
            return result
        }

        // create nodes
        val chain = ArrayList<DynamicBody>(n)
        for (i in 0 until n) {
            chain.add(createPoint(i, if (i == 0 || i == n - 1) 0.0 else 1.0))
        }

        // create links
        for (i in 1 until n) {
            val c0 = chain[i - 1].entity!!
            val c1 = chain[i]
            val dx = 2.0
            c0.addChild(PointConstraint().apply {
                selfPosition = selfPosition.set(-dist * chainFactor, dx, 0.0)
                otherPosition = otherPosition.set(+dist * chainFactor, dx, 0.0)
                other = c1
            })
            c0.addChild(PointConstraint().apply {
                selfPosition = selfPosition.set(-dist * chainFactor, -dx, 0.0)
                otherPosition = otherPosition.set(+dist * chainFactor, -dx, 0.0)
                other = c1
            })
        }

        Systems.world = world

        fun getY(i: Int): Double {
            return chain[i].transform!!.globalPosition.y
        }

        for (k in 0 until 1000) {
            physics.step((0.1f * SECONDS_TO_NANOS).toLong(), false)
            // if (debug) println((chain.indices step 3).map { i -> getY(i).f3s() })
        }

        // trying to fit the chain against a theoretical chain
        fun fitShape(i: Int, y0: Double, y1: Double): Double {
            val halfN = (n - 1) * 0.5
            val x11 = (i - halfN) / halfN
            return mix(y0, y1, (cosh(x11) - cosh1) / (cosh1 - cosh0))
        }

        fun fitShapeError(y0: Double, y1: Double): Double {
            var err = 0.0
            for (i in 1 until n - 1) {
                err += sq(fitShape(i, y0, y1) - getY(i))
            }
            return err
        }

        val (bestErr, bestParams) = simplexAlgorithm(
            doubleArrayOf(0.0, 23.3), 0.3, 0.0, 100
        ) { params -> fitShapeError(params[0], params[1]) }
        val (y0, y1) = bestParams

        if (debug) println(chain.mapIndexed { i, it -> (it.transform!!.globalPosition.y - fitShape(i, y0, y1)).f3s() })
        val avgErr = sqrt(bestErr / (n - 2))

        println("$bestErr -> $avgErr, $y0,$y1")
        assertEquals(7.0, y0, 5.0)
        assertEquals(47.0, y1, 5.0)
        assertTrue(avgErr < 2.3, "$avgErr")
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testBowBridgeFromMeshColliders() {
        val meshes = createBridgeMeshes(8, 0.2f, 1f, 0f)
        val scene = Entity()
        for (i in meshes.indices) {
            val mass = if (i == 0 || i == meshes.lastIndex) 0.0 else 1.0
            val (mesh, pos) = meshes[i]
            Entity(scene)
                .setPosition(pos)
                .add(MeshCollider(mesh).apply { margin = 0f })
                .add(DynamicBody().apply { this.mass = mass })
        }
        val physics = BulletPhysics()
        setupGravityTest(physics, -10f)
        Systems.world = scene
        // withstand a few iterations
        for (i in 0 until 100) {
            val dt = 1f / 60f
            physics.step((dt * SECONDS_TO_NANOS).toLong(), false)
        }
        // validate all positions
        for (i in meshes.indices) {
            val (_, pos) = meshes[i]
            val entity = scene.children[i]
            assertEquals(pos, entity.position, 0.07)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testPhysicsCleanup() {
        val scene = Entity()
        Entity("Floor", scene)
            .add(StaticBody())
            .add(BoxCollider())
            .setPosition(0.0, -10.0, 0.0)
        val rb1 = DynamicBody()
        Entity("Box1", scene)
            .add(rb1)
            .add(BoxCollider())
        val rb2 = DynamicBody()
        Entity("Box2", scene)
            .add(rb2)
            .add(BoxCollider())
            .setPosition(10.0, 0.0, 0.0)
        val physics = BulletPhysics()
        setupGravityTest(physics, -10f)
        Systems.world = scene
        // ensure physics is initialized
        physics.step(SECONDS_TO_NANOS, false)
        // check physics has dynamic objects
        assertEquals(3, physics.rigidBodies.size)
        assertEquals(2, physics.dynamicRigidBodies.size)
        // switch scene
        val emptyScene = Entity()
        Systems.world = emptyScene
        // ensure physics is initialized
        physics.step(SECONDS_TO_NANOS, false)
        assertEquals(0, physics.rigidBodies.size)
        assertEquals(0, physics.dynamicRigidBodies.size)
    }
}