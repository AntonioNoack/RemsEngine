package me.anno.tests.physics.bullet.shapes

import me.anno.bullet.createBulletSphereShape
import me.anno.ecs.components.collider.SphereCollider
import me.anno.sdf.SDFCollider
import me.anno.sdf.physics.ConvexSDFShape
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SDFSphereTest {

    companion object {
        fun Random.nextPos(): Vector3d {
            return Vector3d(nextPosI(), nextPosI(), nextPosI())
        }

        fun Random.nextPosI(): Double {
            return (nextDouble() - 0.5) * 10.0
        }
    }

    @Test
    fun testSDFSupportVector() {
        val baseline = SphereCollider().createBulletSphereShape(Vector3f(1f))
        assertEquals(1f, baseline.margin)
        val tested = ConvexSDFShape(
            SDFSphere().apply {
                localAABB
                    .setMin(-1.0, -1.0, -1.0)
                    .setMax(1.0, 1.0, 1.0)
            }, SDFCollider()
        )
        tested.margin = 0f
        val random = Random(1234)
        repeat(100) {
            val pos = Vector3f(random.nextPos())
            val expected = baseline.localGetSupportingVertex(pos, Vector3f())
            val actual = tested.localGetSupportingVertex(pos, Vector3f())
            assertEquals(expected, actual, 1e-6)
        }
    }
}