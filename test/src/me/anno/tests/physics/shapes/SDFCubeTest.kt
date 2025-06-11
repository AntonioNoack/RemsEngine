package me.anno.tests.physics.shapes

import com.bulletphysics.collision.shapes.BoxShape
import me.anno.bullet.createBulletShape
import me.anno.ecs.components.collider.BoxCollider
import me.anno.sdf.SDFCollider
import me.anno.sdf.physics.ConvexSDFShape
import me.anno.sdf.shapes.SDFBox
import me.anno.tests.physics.shapes.SDFSphereTest.Companion.nextPos
import me.anno.tests.physics.shapes.SDFSphereTest.Companion.toKOML
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SDFCubeTest {

    private fun createBaseline(marginI: Float): BoxShape {
        val baseline = BoxCollider().apply { roundness = marginI }
            .createBulletShape(Vector3d(1.0))
        assertEquals(marginI, baseline.margin.toFloat())
        return baseline
    }

    private fun createTested(marginI: Float): ConvexSDFShape {
        val tested = ConvexSDFShape(
            SDFBox().apply {
                smoothness = marginI
                val di = 1.0 + marginI
                localAABB
                    .setMin(-di, -di, -di)
                    .setMax(di, di, di)
            }, SDFCollider()
        )
        tested.margin = 0.0
        return tested
    }

    @Test
    fun testSDFSupportVector() {
        val baseline = createBaseline(0f)
        val tested = createTested(0f)
        val random = Random(1234)
        for (i in 0 until 20) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d()).toKOML()
            val actual = tested.localGetSupportingVertex(pos, Vector3d()).toKOML()
            assertEquals(expected, actual, 1.0) // error is a little large...
        }
    }

    @Test
    fun testSDFSupportVectorCorners() {
        val baseline = createBaseline(0f)
        val tested = createTested(0f)
        for (i in 0 until 8) {
            val pos = Vector3d(
                if (i.hasFlag(1)) 1.0 else -1.0,
                if (i.hasFlag(2)) 1.0 else -1.0,
                if (i.hasFlag(4)) 1.0 else -1.0,
            )
            val expected = baseline.localGetSupportingVertex(pos, Vector3d()).toKOML()
            val actual = tested.localGetSupportingVertex(pos, Vector3d()).toKOML()
            assertEquals(expected, actual, 1e-7)
        }
    }

    @Test
    fun testSDFSupportVectorWithMargin() {
        val baseline = createBaseline(1f)
        val tested = createTested(1f)
        val random = Random(1234)
        for (i in 0 until 100) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d()).toKOML()
            val actual = tested.localGetSupportingVertex(pos, Vector3d()).toKOML()
            assertEquals(expected, actual, 2.0, "$pos") // error is a little large...
        }
    }
}