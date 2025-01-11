package me.anno.tests.geometry

import me.anno.maths.geometry.Polygons
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Triangles
import me.anno.utils.types.Triangles.crossDot
import me.anno.utils.types.Triangles.getBarycentrics
import me.anno.utils.types.Triangles.getSideSign
import me.anno.utils.types.Triangles.getTriangleArea
import me.anno.utils.types.Triangles.halfSubCrossDot
import me.anno.utils.types.Triangles.isInsideTriangle
import me.anno.utils.types.Triangles.linePointTFactor
import me.anno.utils.types.Triangles.rayTriangleIntersection
import me.anno.utils.types.Triangles.rayTriangleIntersectionFront
import me.anno.utils.types.Triangles.subCross
import me.anno.utils.types.Triangles.subCrossDot
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class TriangleTest {

    private val numTries = 100

    companion object {
        fun Vector2f.isInsideTriangleBaseline(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {
            var sum = 0
            if (getSideSign(a, b) > 0f) sum++
            if (getSideSign(b, c) > 0f) sum++
            if (getSideSign(c, a) > 0f) sum++
            // left or right of all lines
            return sum == 0 || sum == 3
        }

        fun Vector2d.isInsideTriangleBaseline(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {
            var sum = 0
            if (getSideSign(a, b) > 0f) sum++
            if (getSideSign(b, c) > 0f) sum++
            if (getSideSign(c, a) > 0f) sum++
            // left or right of all lines
            return sum == 0 || sum == 3
        }
    }

    @Test
    fun testSubCross2f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector2f(random.nextFloat(), random.nextFloat())
            val b = Vector2f(random.nextFloat(), random.nextFloat())
            val c = Vector2f(random.nextFloat(), random.nextFloat())
            val expected = ((b - a).cross(c - a))
            val actual = subCross(a, b, c)
            assertEquals(expected, actual, 1e-7f)
        }
    }

    @Test
    fun testCrossDot3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val ab = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val ac = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val n = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val expected = (ab).cross(ac, Vector3f()).dot(n)
            val actual = crossDot(ab, ac, n)
            assertEquals(expected, actual, 1e-7f)
            val actual2 = crossDot(ab.x, ab.y, ab.z, ac.x, ac.y, ac.z, n.x, n.y, n.z)
            assertEquals(expected, actual2, 1e-7f)
        }
    }

    @Test
    fun testSubCross3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val expected = ((b - a).cross(c - a))
            val actual = subCross(a, b, c, Vector3f())
            assertEquals(expected, actual, 1e-7)
        }
    }

    @Test
    fun testSubCrossDot3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val n = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val expected = ((b - a).cross(c - a).dot(n))
            val actual = subCrossDot(a, b, c, n)
            assertEquals(expected, actual, 1e-7f)
        }
    }

    @Test
    fun testSubCrossDot3d() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val b = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val c = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val n = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val expected = ((b - a).cross(c - a).dot(n))
            val actual = subCrossDot(a, b, c, n)
            assertEquals(expected, actual, 1e-16)
        }
    }

    @Test
    fun testHalfSubCrossDot3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val n = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val expected = ((b - a).cross(c - a).dot(n))
            val actual = halfSubCrossDot(b - a, a, c, n)
            assertEquals(expected, actual, 1e-7f)
        }
    }

    @Test
    fun testTriangleArea3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val expected = Polygons.getPolygonArea3d(listOf(a, b, c).map { Vector3d(it) }).toFloat()
            val actual = getTriangleArea(a, b, c)
            assertEquals(expected, actual, 1e-7f)
            assertEquals(expected, Triangles.getParallelogramArea(a, b, c) * 0.5f, 1e-7f)
        }
    }

    @Test
    fun testTriangleArea2f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val a = Vector2f(random.nextFloat(), random.nextFloat())
            val b = Vector2f(random.nextFloat(), random.nextFloat())
            val c = Vector2f(random.nextFloat(), random.nextFloat())
            val expected = Polygons.getPolygonArea2f(listOf(a, b, c))
            val actual = getTriangleArea(a, b, c)
            assertEquals(expected, actual, 1e-7f)
            assertEquals(expected, Triangles.getParallelogramArea(a, b, c) * 0.5f, 1e-7f)
        }
    }

    @Test
    fun testGetBarycentrics2f() {
        val e = 1e-5
        val exp = Vector3f()
        val tmp = Vector3f()
        val random = Random(1234)
        val a = Vector2f()
        val b = Vector2f()
        val c = Vector2f()
        for (i in 0 until numTries) {
            do {
                a.set(random.nextFloat(), random.nextFloat())
                b.set(random.nextFloat(), random.nextFloat())
                c.set(random.nextFloat(), random.nextFloat())
            } while (abs(getTriangleArea(a, b, c)) < 0.05f)
            val ab = (a + b) * 0.5f
            val bc = (b + c) * 0.5f
            val ca = (c + a) * 0.5f
            val m = (a + b + c) * (1f / 3f)
            assertEquals(exp.set(1f, 0f, 0f), getBarycentrics(a, b, c, a, tmp), e)
            assertEquals(exp.set(0f, 1f, 0f), getBarycentrics(a, b, c, b, tmp), e)
            assertEquals(exp.set(0f, 0f, 1f), getBarycentrics(a, b, c, c, tmp), e)
            assertEquals(exp.set(0.5f, 0.5f, 0f), getBarycentrics(a, b, c, ab, tmp), e)
            assertEquals(exp.set(0f, 0.5f, 0.5f), getBarycentrics(a, b, c, bc, tmp), e)
            assertEquals(exp.set(0.5f, 0f, 0.5f), getBarycentrics(a, b, c, ca, tmp), e)
            assertEquals(exp.set(1f / 3f, 1f / 3f, 1f / 3f), getBarycentrics(a, b, c, m, tmp), e)
        }
    }

    @Test
    fun testGetBarycentrics3f() {
        val e = 1e-4
        val exp = Vector3f()
        val tmp = Vector3f()
        val random = Random(1234)
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        for (i in 0 until numTries) {
            do {
                a.set(random.nextFloat(), random.nextFloat(), random.nextFloat())
                b.set(random.nextFloat(), random.nextFloat(), random.nextFloat())
                c.set(random.nextFloat(), random.nextFloat(), random.nextFloat())
            } while (abs(getTriangleArea(a, b, c)) < 0.05f)
            val ab = (a + b) * 0.5f
            val bc = (b + c) * 0.5f
            val ca = (c + a) * 0.5f
            val m = (a + b + c) * (1f / 3f)
            assertEquals(exp.set(1f, 0f, 0f), getBarycentrics(a, b, c, a, tmp), e)
            assertEquals(exp.set(0f, 1f, 0f), getBarycentrics(a, b, c, b, tmp), e)
            assertEquals(exp.set(0f, 0f, 1f), getBarycentrics(a, b, c, c, tmp), e)
            assertEquals(exp.set(0.5f, 0.5f, 0f), getBarycentrics(a, b, c, ab, tmp), e)
            assertEquals(exp.set(0f, 0.5f, 0.5f), getBarycentrics(a, b, c, bc, tmp), e)
            assertEquals(exp.set(0.5f, 0f, 0.5f), getBarycentrics(a, b, c, ca, tmp), e)
            assertEquals(exp.set(1f / 3f, 1f / 3f, 1f / 3f), getBarycentrics(a, b, c, m, tmp), e)
        }
    }

    @Test
    fun testIsInsideTriangle2f() {
        val random = Random(1234)
        val a = Vector2f()
        val b = Vector2f()
        val c = Vector2f()
        val uvw = Vector3f()
        val point = Vector2f()
        for (i in 0 until numTries) {
            a.set(random.nextFloat(), random.nextFloat())
            b.set(random.nextFloat(), random.nextFloat())
            c.set(random.nextFloat(), random.nextFloat())
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsideTriangle(a, b, c)
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testIsInsideTriangle2d() {
        val random = Random(1234)
        val a = Vector2d()
        val b = Vector2d()
        val c = Vector2d()
        val uvw = Vector3d()
        val point = Vector2d()
        for (i in 0 until numTries) {
            a.set(random.nextDouble(), random.nextDouble())
            b.set(random.nextDouble(), random.nextDouble())
            c.set(random.nextDouble(), random.nextDouble())
            getRandomUVW(random, 1.5, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsideTriangle(a, b, c)
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testLinePointTFactor3f() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val start = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dir = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize()
            val t = random.nextFloat() * 10f - 5f
            val expectedT = max(t, 0f)
            val point = start + dir * expectedT
            val actualT = linePointTFactor(start, dir, point.x, point.y, point.z)
            assertEquals(expectedT, actualT, 1e-5f)
        }
    }

    @Test
    fun testLinePointTFactor3d() {
        val random = Random(1234)
        for (i in 0 until numTries) {
            val start = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val dir = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble()).normalize()
            val t = random.nextDouble() * 10.0 - 5.0
            val expectedT = max(t, 0.0)
            val point = start + dir * expectedT
            val actualT = linePointTFactor(start, dir, point.x, point.y, point.z)
            assertEquals(expectedT, actualT, 1e-14)
        }
    }

    @Test
    fun testRayTriangleIntersection3f() {
        val random = Random(1234)
        val dstNormal = Vector3f()
        val dstPosition = Vector3f()
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dir = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize()
            val testTooFar = i == 0
            val uvw = getRandomUVW(random, if (testTooFar) 0.9f else 1.2f)
            val position = interpolateBarycentric(a, b, c, uvw, Vector3f())
            val distance = random.nextFloat() * 10f
            val start = position - dir * distance
            val dstUVW0 = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dstUVW = Vector3f(dstUVW0)
            val hit = isValidUVW(uvw) && !testTooFar
            val distanceLimit = if (testTooFar) distance * 0.99f else 1e3f
            val expectedDist = if (hit) distance else Float.POSITIVE_INFINITY
            val actualDist = rayTriangleIntersection(start, dir, a, b, c, distanceLimit, dstNormal, dstPosition, dstUVW)
            assertEquals(min(expectedDist, 1e38f), min(actualDist, 1e38f), 1e-3f)
            if (hit) assertEquals(uvw, dstUVW, 1e-3)
            else assertEquals(dstUVW0, dstUVW) // must not be changed, if the ray doesn't hit
        }
    }

    @Test
    fun testRayTriangleIntersection3d() {
        val random = Random(1234)
        val dstNormal = Vector3d()
        val dstPosition = Vector3d()
        for (i in 0 until numTries) {
            val a = Vector3d(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3d(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3d(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dir = Vector3d(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize()
            val testTooFar = i == 0
            val uvw = getRandomUVW(random, if (testTooFar) 0.9 else 1.2)
            val position = interpolateBarycentric(a, b, c, uvw, Vector3d())
            val distance = random.nextDouble() * 10.0
            val start = position - dir * distance
            val dstUVW0 = Vector3d(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dstUVW = Vector3d(dstUVW0)
            val hit = isValidUVW(uvw) && !testTooFar
            val distanceLimit = if (testTooFar) distance * 0.99 else 1e3
            val expectedDist = if (hit) distance else Double.POSITIVE_INFINITY
            val actualDist = rayTriangleIntersection(start, dir, a, b, c, distanceLimit, dstNormal, dstPosition, dstUVW)
            assertEquals(min(expectedDist, 1e300), min(actualDist, 1e300), 1e-12)
            if (hit) assertEquals(uvw, dstUVW, 1e-11)
            else assertEquals(dstUVW0, dstUVW) // must not be changed, if the ray doesn't hit
        }
    }

    @Test
    fun testRayTriangleIntersectionFront3f() {
        val random = Random(1234)
        val dstNormal = Vector3f()
        val dstPosition = Vector3f()
        for (i in 0 until numTries) {
            val a = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val b = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val c = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dir = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize()
            val uvw = getRandomUVW(random, 1f)
            val position = interpolateBarycentric(a, b, c, uvw, Vector3f())
            val distance = random.nextFloat() * 10f
            val start = position - dir * distance
            val dstUVW0 = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
            val dstUVW = Vector3f(dstUVW0)
            val hit = subCrossDot(a, b, c, dir) < 0.0
            val distanceLimit = 1e3f
            val expectedDist = if (hit) distance else Float.POSITIVE_INFINITY
            val actualDist =
                rayTriangleIntersectionFront(start, dir, a, b, c, distanceLimit, dstNormal, dstPosition, dstUVW)
            assertEquals(min(expectedDist, 1e38f), min(actualDist, 1e38f), 1e-3f)
            if (hit) assertEquals(uvw, dstUVW, 1e-3)
            else assertEquals(dstUVW0, dstUVW) // must not be changed, if the ray doesn't hit
        }
    }

    @Test
    fun testRayTriangleIntersectionFront3d() {
        val random = Random(1234)
        val dstNormal = Vector3d()
        val dstPosition = Vector3d()
        for (i in 0 until numTries) {
            val a = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val b = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val c = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val dir = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble()).normalize()
            val uvw = getRandomUVW(random, 1.0)
            val position = interpolateBarycentric(a, b, c, uvw, Vector3d())
            val distance = random.nextDouble() * 10.0
            val start = position - dir * distance
            val dstUVW0 = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
            val dstUVW = Vector3d(dstUVW0)
            val hit = subCrossDot(a, b, c, dir) < 0.0
            val distanceLimit = 1e3
            val expectedDist = if (hit) distance else Double.POSITIVE_INFINITY
            val actualDist =
                rayTriangleIntersectionFront(start, dir, a, b, c, distanceLimit, dstNormal, dstPosition, dstUVW)
            assertEquals(min(expectedDist, 1e300), min(actualDist, 1e300), 1e-12)
            if (hit) assertEquals(uvw, dstUVW, 1e-11)
            else assertEquals(dstUVW0, dstUVW) // must not be changed, if the ray doesn't hit
        }
    }

    fun <V : Vector> interpolateBarycentric(a: V, b: V, c: V, uvw: Vector, dst: V): V {
        assertEquals(3, uvw.numComponents)
        assertEquals(a.numComponents, b.numComponents)
        assertEquals(a.numComponents, c.numComponents)
        assertEquals(a.numComponents, dst.numComponents)
        for (i in 0 until a.numComponents) {
            val au = a.getComp(i) * uvw.getComp(0)
            val bv = b.getComp(i) * uvw.getComp(1)
            val cw = c.getComp(i) * uvw.getComp(2)
            dst.setComp(i, au + bv + cw)
        }
        return dst
    }

    fun isValidUVW(uvw: Vector): Boolean {
        assertEquals(3, uvw.numComponents)
        return uvw.getComp(0) in 0.0..1.0 &&
                uvw.getComp(1) in 0.0..1.0 &&
                uvw.getComp(2) in 0.0..1.0
    }

    fun getRandomUVW(random: Random, scale: Float, uvw: Vector3f = Vector3f()): Vector3f {
        return uvw.set(getRandomUVW(random, scale.toDouble()))
    }

    fun getRandomUVW(random: Random, scale: Double, uvw: Vector3d = Vector3d()): Vector3d {
        do {
            uvw.set(random.nextDouble(), random.nextDouble(), random.nextDouble())
        } while (uvw.x + uvw.y + uvw.z > 1.0)
        uvw.sub(0.5).mul(scale).add(0.5)
        uvw.z = 1.0 - (uvw.x + uvw.y)
        return uvw
    }
}