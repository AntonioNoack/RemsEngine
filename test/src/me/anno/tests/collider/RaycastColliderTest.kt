package me.anno.tests.collider

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.showDebugArrow
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RaycastCollider
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.TAUf
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFCapsule
import me.anno.sdf.shapes.SDFCone
import me.anno.sdf.shapes.SDFCylinder
import me.anno.sdf.shapes.SDFPlane
import me.anno.sdf.shapes.SDFShape
import me.anno.sdf.shapes.SDFSphere
import me.anno.ui.UIColors
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RaycastColliderTest {

    @Test
    fun testRaycastingSphereCollider() {
        testRaycastingCollider(SphereCollider(), SDFSphere(), 100)
        // testDebugImage(SphereCollider(), SDFSphere())
    }

    @Test
    fun testRaycastingBoxCollider() {
        testRaycastingCollider(
            BoxCollider().apply { roundness = 0f },
            SDFBox().apply { smoothness = 0f }, 93
        )
    }

    @Test
    fun testRaycastingConeCollider() {
        testRaycastingCollider(ConeCollider().apply { axis = Axis.X }, SDFCone().apply { axis = Axis.X }, 99)
        testRaycastingCollider(ConeCollider().apply { axis = Axis.Y }, SDFCone().apply { axis = Axis.Y }, 99)
        testRaycastingCollider(ConeCollider().apply { axis = Axis.Z }, SDFCone().apply { axis = Axis.Z }, 99)
    }

    @Test
    fun testRaycastingCylinderCollider() {
        testRaycastingCollider(CylinderCollider().apply { axis = Axis.X }, SDFCylinder().apply { axis = Axis.X }, 99)
        testRaycastingCollider(CylinderCollider().apply { axis = Axis.Y }, SDFCylinder().apply { axis = Axis.Y }, 98)
        testRaycastingCollider(CylinderCollider().apply { axis = Axis.Z }, SDFCylinder().apply { axis = Axis.Z }, 98)
    }

    @Test
    fun testRaycastingCapsuleCollider() {
        testRaycastingCollider(
            CapsuleCollider().apply { axis = Axis.X },
            SDFCapsule().apply {
                p0.set(-1f, 0f, 0f)
                p1.set(1f, 0f, 0f)
            }, 97
        )
        testRaycastingCollider(
            CapsuleCollider().apply { axis = Axis.Y },
            SDFCapsule().apply {
                p0.set(0f, -1f, 0f)
                p1.set(0f, 1f, 0f)
            }, 97
        )
        testRaycastingCollider(
            CapsuleCollider().apply { axis = Axis.Z },
            SDFCapsule().apply {
                p0.set(0f, 0f, -1f)
                p1.set(0f, 0f, 1f)
            }, 97
        )
    }

    @Test
    fun testRaycastingPlaneCollider() {
        testRaycastingCollider(InfinitePlaneCollider(), SDFPlane(), 79)
        if (false) testDebugImage(InfinitePlaneCollider(), SDFPlane())
    }

    fun test(pos: Vector3d, dir: Vector3f, collider: Collider): Boolean {
        val entity = Entity()
        val query0 = RayQuery(pos, dir, 1e9)
        return RaycastCollider.raycastGlobalCollider(query0, entity, collider)
    }

    fun test(pos: Vector3d, dir: Vector3f, sdf: SDFShape): Boolean {
        val query1 = RayQuery(pos, dir, 1e9)
        return sdf.raycast(query1)
    }

    fun Vector3f.randomDir(random: Random) {
        set(1f, 0f, 0f)
            .rotateY(random.nextFloat() * TAUf)
            .rotateX(random.nextFloat() * TAUf)
            .rotateZ(random.nextFloat() * TAUf)
    }

    fun testRaycastingCollider(collider: Collider, sdf: SDFShape, quality: Int) {

        val entity = Entity()
        val random = Random(1234)

        assertTrue(collider.isConvex)

        val pos = Vector3d()
        val dir = Vector3f()
        if (collider !is InfinitePlaneCollider) {
            repeat(16) {

                dir.randomDir(random)
                pos.set(dir).mul(-5.0)

                assertTrue(test(pos, dir, collider))
                assertTrue(test(pos, dir, sdf))

                pos.set(dir).mul(5.0)

                assertFalse(test(pos, dir, collider))
                assertFalse(test(pos, dir, sdf))
            }
        }

        var same = 0
        val tries = 1000
        repeat(tries) {

            pos.set(random.nextDouble(), random.nextDouble(), random.nextDouble())
                .sub(0.5).mul(5.0)

            dir.randomDir(random)

            val query0 = RayQuery(pos, dir, 1e9)
            val query1 = RayQuery(pos, dir, 1e9)
            val hit0 = RaycastCollider.raycastGlobalCollider(query0, entity, collider)
            val hit1 = sdf.raycast(query1)

            if (hit0 == hit1) {
                same++

                assertEquals(hit1, hit0)
                // todo how can these give soo incorrect results???
                // assertEquals(query1.result.distance, query0.result.distance, 0.3)
                // assertEquals(query1.result.positionWS, query0.result.positionWS, 0.3)
            }
        }
        assertGreaterThanEquals(same, tries * quality / 100)
        println("Success: $same/$tries")
    }

    fun testDebugImage(collider: Collider, sdf: SDFShape) {

        // todo create image
        val entity = Entity()
        val random = Random(1234)

        assertTrue(collider.isConvex)

        val stats = IntArray(4)

        repeat(1000) {

            val pos = Vector3d()
            val dir = Vector3f()

            pos.set(random.nextDouble(), random.nextDouble(), random.nextDouble())
                .sub(0.5).mul(2.0)

            dir.set(random.nextFloat(), random.nextFloat(), random.nextFloat())
                .sub(0.5f).normalize()

            val query0 = RayQuery(pos, dir, 1e3)
            val query1 = RayQuery(pos, dir, 1e3)
            val hit0 = RaycastCollider.raycastGlobalCollider(query0, entity, collider)
            val hit1 = sdf.raycast(query1)

            if (hit0 != hit1) {
                // bad rays
                if (hit0) {
                    showDebugArrow(DebugLine(pos, query0.result.positionWS, UIColors.dodgerBlue, 1e3f))
                }
                if (hit1) {
                    showDebugArrow(DebugLine(pos, query1.result.positionWS, UIColors.midOrange, 1e3f))
                }
            } else {
                // good rays
                if (hit0) {
                    showDebugArrow(DebugLine(pos, query0.result.positionWS, UIColors.blueishGray, 1e3f))
                } else {
                    showDebugArrow(
                        DebugLine(
                            pos,
                            Vector3d(dir).mul(0.1).add(pos),
                            UIColors.fireBrick,
                            1e3f
                        )
                    )
                }
            }

            stats[hit0.toInt() + hit1.toInt(2)]++
        }

        println("Stats: ${stats.toList()}")

        testSceneWithUI("Collider", entity.add(collider).add(sdf))
    }
}