package me.anno.tests.gfx

import me.anno.engine.ui.render.Frustum
import me.anno.maths.noise.RandomUtils.nextGaussianF
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.Quaternionf
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random

class FrustumTests {

    @Test
    fun testAllBounds() {
        val frustum = Frustum()
        val random = Random(165)
        val allBounds = AABBd().all()
        repeat(100) {
            frustum.definePerspective(
                0.1f, 10f, 1f, 100, 1f,
                Vector3d(
                    random.nextGaussianF(),
                    random.nextGaussianF(),
                    random.nextGaussianF()
                ), Quaternionf().rotationTo(
                    random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f
                )
            )
            assertTrue(frustum.isVisible(allBounds))
        }
    }

    @Test
    fun simpleTest() {
        val frustum = Frustum()
        val res = 100

        frustum.definePerspective(
            0.001f, 100.0f, (90f).toRadians(), res, 1f, Vector3d(0.0, 0.0, -1.0),
            Quaternionf()
        )

        val aabb1 = AABBd()
        aabb1.union(0.0, 0.0, 0.0)

        val aabb2 = AABBd()
        aabb2.union(0.0, 0.0, 0.9)

        assertFalse(aabb1 in frustum)
        assertFalse(aabb2 in frustum)

        frustum.definePerspective(
            0.001f, 100f, (90f).toRadians(), res, 1f, Vector3d(0.0, 0.0, 1.0),
            Quaternionf()
        )

        assertTrue(aabb1 in frustum)
        assertTrue(aabb2 in frustum)
    }
}
