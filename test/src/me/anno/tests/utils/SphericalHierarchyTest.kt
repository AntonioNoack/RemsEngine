package me.anno.tests.utils

import me.anno.maths.chunks.spherical.SphereTriangle
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4x3
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class SphericalHierarchyTest {
    @Test
    fun testCoordinateTransform() {
        val a = Vector3d(-1.0, -1.0, 1.0)
        val b = Vector3d(-1.0, +1.0, 1.0)
        val c = Vector3d(+2.0, +0.0, 1.0)
        val tri = SphereTriangle(null, 0, 0, a, b, c)
        // strategic tests
        assertEquals(Vector3f(0f, 1f, 0f), tri.baseAB)
        assertEquals(Vector3f(0f, 0f, 1f), tri.baseUp)
        assertEquals(Vector3f(1f, 0f, 0f), tri.baseAC)
        assertEquals(tri.baseAB, tri.localToGlobal.transformDirection(Vector3f(1f, 0f, 0f)))
        assertEquals(tri.baseUp, tri.localToGlobal.transformDirection(Vector3f(0f, 1f, 0f)))
        assertEquals(tri.baseAC, tri.localToGlobal.transformDirection(Vector3f(0f, 0f, 1f)))
        assertEquals(tri.globalToLocal, tri.localToGlobal.invert(Matrix4x3()))
        assertEquals(tri.localA, tri.globalToLocal.transformPosition(a, Vector3d()))
        assertEquals(tri.localB, tri.globalToLocal.transformPosition(b, Vector3d()))
        assertEquals(tri.localC, tri.globalToLocal.transformPosition(c, Vector3d()))
        // some random on-paper checks
        assertEquals(
            Vector3d(0.0, 0.1, 0.0),
            tri.globalToLocal.transformPosition(Vector3d(0.0, 0.0, 1.1)),
            1e-15
        )
        assertEquals(
            Vector3d(1.0, 0.1, 0.0),
            tri.globalToLocal.transformPosition(Vector3d(0.0, 1.0, 1.1)),
            1e-15
        )
    }
}