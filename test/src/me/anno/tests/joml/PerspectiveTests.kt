package me.anno.tests.joml

import me.anno.gpu.drawing.Perspective
import me.anno.maths.Maths.PIf
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4f
import org.joml.Vector3f
import org.junit.jupiter.api.Test

/**
 * this is partially JOML, partially custom
 * */
class PerspectiveTests {

    private val near = 0.1f
    private val far = 100f
    private val fovRadians = PIf * 0.5f
    private val aspect = 2.5f

    private val iN = 1f / aspect

    @Test
    fun testSetPerspectiveNormal() {
        val m = Matrix4f()
        Perspective.setPerspective(m, fovRadians, aspect, near, far, 0f, 0f, false)
        testPerspective(m, expectedPositions(far, -1f, +1f), far)
    }

    @Test
    fun testSetPerspectiveReverse() {
        val m = Matrix4f()
        // far is ignored, so we can pass NaN to check that it truly is ignored ^^
        Perspective.setPerspective(m, fovRadians, aspect, near, Float.NaN, 0f, 0f, true)
        val far = 1e38f // close to infinity, but not exactly, so we don't get NaNs
        testPerspective(m, expectedPositions(far, 1f, 0f), far)
    }

    private fun expectedPositions(far: Float, zNear: Float, zFar: Float): List<Vector3f> {
        return listOf(
            // center
            Vector3f(0f, 0f, zNear),
            Vector3f(0f, 0f, zFar),
            // top left
            Vector3f(-iN / near, -1f / near, zNear),
            Vector3f(-iN / far, -1f / far, zFar),
            // top right
            Vector3f(+iN / near, -1f / near, zNear),
            Vector3f(+iN / far, -1f / far, zFar),
            // bottom left
            Vector3f(-iN / near, +1f / near, zNear),
            Vector3f(-iN / far, +1f / far, zFar),
            // bottom right
            Vector3f(+iN / near, +1f / near, zNear),
            Vector3f(+iN / far, +1f / far, zFar),
        )
    }

    fun testPerspective(m: Matrix4f, corners: List<Vector3f>, far: Float) {
        val inCorners = listOf(
            // center
            Vector3f(0f, 0f, -near),
            Vector3f(0f, 0f, -far),
            // top left
            Vector3f(-1f, -1f, -near),
            Vector3f(-1f, -1f, -far),
            // top right
            Vector3f(1f, -1f, -near),
            Vector3f(1f, -1f, -far),
            // bottom left
            Vector3f(-1f, +1f, -near),
            Vector3f(-1f, +1f, -far),
            // bottom right
            Vector3f(1f, +1f, -near),
            Vector3f(1f, +1f, -far),
        )
        for (vi in inCorners.indices) {
            val inCorner = inCorners[vi]
            val actual = m.transformProject(inCorner, Vector3f())
            val expected = corners[vi]
            assertEquals(expected, actual, 1e-6, "$inCorner[$vi]")
        }
    }
}