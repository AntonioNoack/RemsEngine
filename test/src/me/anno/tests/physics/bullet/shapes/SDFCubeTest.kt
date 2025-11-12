package me.anno.tests.physics.bullet.shapes

import com.bulletphysics.collision.shapes.BoxShape
import me.anno.bullet.createBulletBoxShape
import me.anno.ecs.components.collider.BoxCollider
import me.anno.sdf.SDFCollider
import me.anno.sdf.physics.ConvexSDFShape
import me.anno.sdf.shapes.SDFBox
import me.anno.tests.physics.bullet.shapes.SDFSphereTest.Companion.nextPos
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SDFCubeTest {

    private fun createBaseline(marginI: Float): BoxShape {
        val baseline = BoxCollider().apply { roundness = marginI }
            .createBulletBoxShape(Vector3f(1f))
        assertEquals(marginI, baseline.margin)
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
        tested.margin = 0f
        return tested
    }

    @Test
    fun testSDFSupportVector() {
        val baseline = createBaseline(0f)
        val tested = createTested(0f)
        val random = Random(1234)
        repeat(20) {
            val pos = Vector3f(random.nextPos())
            val expected = baseline.localGetSupportingVertex(pos, Vector3f())
            val actual = tested.localGetSupportingVertex(pos, Vector3f())
            assertEquals(expected, actual, 1.0) // error is a little large...
        }
    }

    @Test
    fun testSDFSupportVectorCorners() {
        val baseline = createBaseline(0f)
        val tested = createTested(0f)
        for (i in 0 until 8) {
            val pos = Vector3f(
                if (i.hasFlag(1)) 1f else -1f,
                if (i.hasFlag(2)) 1f else -1f,
                if (i.hasFlag(4)) 1f else -1f,
            )
            val expected = baseline.localGetSupportingVertex(pos, Vector3f())
            val actual = tested.localGetSupportingVertex(pos, Vector3f())
            assertEquals(expected, actual, 1e-7)
        }
    }

    @Test
    fun testSDFSupportVectorWithMargin() {
        val baseline = createBaseline(1f)
        val tested = createTested(1f)
        val random = Random(1234)
        repeat(100) {
            val pos = Vector3f(random.nextPos())
            val expected = baseline.localGetSupportingVertex(pos, Vector3f())
            val actual = tested.localGetSupportingVertex(pos, Vector3f())
            assertEquals(expected, actual, 2.0, "$pos") // error is a little large...
        }
    }
}