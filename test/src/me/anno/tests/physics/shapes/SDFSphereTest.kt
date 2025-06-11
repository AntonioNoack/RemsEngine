package me.anno.tests.physics.shapes

import me.anno.bullet.createBulletShape
import me.anno.ecs.components.collider.SphereCollider
import me.anno.sdf.SDFCollider
import me.anno.sdf.physics.ConvexSDFShape
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SDFSphereTest {

    companion object {
        fun Vector3d.toKOML(): Vector3d {
            return Vector3d(x, y, z)
        }

        fun Random.nextPos(): Vector3d {
            return Vector3d(nextPosI(), nextPosI(), nextPosI())
        }

        fun Random.nextPosI(): Double {
            return (nextDouble() - 0.5) * 10.0
        }
    }

    @Test
    fun testSDFSupportVector() {
        val baseline = SphereCollider().createBulletShape(Vector3d(1.0))
        assertEquals(1.0, baseline.margin)
        val tested = ConvexSDFShape(
            SDFSphere().apply {
                localAABB
                    .setMin(-1.0, -1.0, -1.0)
                    .setMax(1.0, 1.0, 1.0)
            }, SDFCollider()
        )
        tested.margin = 0.0
        val random = Random(1234)
        for (i in 0 until 100) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d()).toKOML()
            val actual = tested.localGetSupportingVertex(pos, Vector3d()).toKOML()
            assertEquals(expected, actual, 1e-6)
        }
    }
}