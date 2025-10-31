package me.anno.tests.physics.box2d

import me.anno.box2d.CircleCollider
import me.anno.box2d.RectCollider
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class Box2dSDFTests {
    @Test
    fun testCircleColliderSDF() {
        val c = CircleCollider()
        c.radius = 2f
        assertEquals(3f, c.getSignedDistance(Vector3f(5f, 0f, 0f)))
        assertEquals(3f, c.getSignedDistance(Vector3f(0f, 5f, 0f)))
        assertEquals(-2f, c.getSignedDistance(Vector3f(0f)))
    }

    @Test
    fun testRectColliderSDF() {
        val r = RectCollider()
        r.halfExtents.set(1f, 2f)
        assertEquals(0f, r.getSignedDistance(Vector3f(1f, 2f, 0f)))
        assertEquals(-1f, r.getSignedDistance(Vector3f(0f)))
        assertEquals(1f, r.getSignedDistance(Vector3f(2f, 2f, 0f)))
        assertEquals(1f, r.getSignedDistance(Vector3f(1f, 3f, 0f)))
        val sqrt2 = sqrt(2f)
        assertEquals(2f, r.getSignedDistance(Vector3f(1f + sqrt2, 2f + sqrt2, 0f)), 1e-6f)
    }
}