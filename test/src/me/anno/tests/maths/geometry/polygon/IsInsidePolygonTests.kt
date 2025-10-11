package me.anno.tests.maths.geometry.polygon

import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.maths.geometry.polygon.IsInsidePolygon.isInsidePolygon
import me.anno.tests.geometry.TriangleTest.Companion.getRandomUVW
import me.anno.tests.geometry.TriangleTest.Companion.interpolateBarycentric
import me.anno.tests.geometry.TriangleTest.Companion.isInsideTriangleBaseline
import me.anno.utils.assertions.assertEquals
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class IsInsidePolygonTests {

    val numTries = 1000

    @Test
    fun testInsidePolygon2i() {
        val random = Random(1234)
        val scale = 1000_000_000
        val a = Vector2i()
        val b = Vector2i()
        val c = Vector2i()
        val uvw = Vector3f()
        val point = Vector2i()
        repeat(numTries) {
            a.set(random.nextInt(scale), random.nextInt(scale))
            b.set(random.nextInt(scale), random.nextInt(scale))
            c.set(random.nextInt(scale), random.nextInt(scale))
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(listOf(a, b, c))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon2iA() {
        val random = Random(1234)
        val scale = 1000_000_000
        val a = Vector2i()
        val b = Vector2i()
        val c = Vector2i()
        val uvw = Vector3f()
        val point = Vector2i()
        repeat(numTries) {
            a.set(random.nextInt(scale), random.nextInt(scale))
            b.set(random.nextInt(scale), random.nextInt(scale))
            c.set(random.nextInt(scale), random.nextInt(scale))
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(intArrayOf(a.x, a.y, b.x, b.y, c.x, c.y))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon2f() {
        val random = Random(1234)
        val a = Vector2f()
        val b = Vector2f()
        val c = Vector2f()
        val uvw = Vector3f()
        val point = Vector2f()
        repeat(numTries) {
            a.set(random.nextFloat(), random.nextFloat())
            b.set(random.nextFloat(), random.nextFloat())
            c.set(random.nextFloat(), random.nextFloat())
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(listOf(a, b, c))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon2fA() {
        val random = Random(1234)
        val a = Vector2f()
        val b = Vector2f()
        val c = Vector2f()
        val uvw = Vector3f()
        val point = Vector2f()
        repeat(numTries) {
            a.set(random.nextFloat(), random.nextFloat())
            b.set(random.nextFloat(), random.nextFloat())
            c.set(random.nextFloat(), random.nextFloat())
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0f..1f && uvw.y in 0f..1f && uvw.z in 0f..1f
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(floatArrayOf(a.x, a.y, b.x, b.y, c.x, c.y))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon2d() {
        val random = Random(1234)
        val a = Vector2d()
        val b = Vector2d()
        val c = Vector2d()
        val uvw = Vector3d()
        val point = Vector2d()
        repeat(numTries) {
            a.set(random.nextDouble(), random.nextDouble())
            b.set(random.nextDouble(), random.nextDouble())
            c.set(random.nextDouble(), random.nextDouble())
            getRandomUVW(random, 1.5, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0.0..1.0 && uvw.y in 0.0..1.0 && uvw.z in 0.0..1.0
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(listOf(a, b, c))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon2dA() {
        val random = Random(1234)
        val a = Vector2d()
        val b = Vector2d()
        val c = Vector2d()
        val uvw = Vector3d()
        val point = Vector2d()
        repeat(numTries) {
            a.set(random.nextDouble(), random.nextDouble())
            b.set(random.nextDouble(), random.nextDouble())
            c.set(random.nextDouble(), random.nextDouble())
            getRandomUVW(random, 1.5, uvw)
            interpolateBarycentric(a, b, c, uvw, point)
            val expected = uvw.x in 0.0..1.0 && uvw.y in 0.0..1.0 && uvw.z in 0.0..1.0
            val expected2 = point.isInsideTriangleBaseline(a, b, c)
            val actual = point.isInsidePolygon(doubleArrayOf(a.x, a.y, b.x, b.y, c.x, c.y))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon3f() {
        val random = Random(1234)
        val a0 = Vector2f()
        val b0 = Vector2f()
        val c0 = Vector2f()
        val uvw = Vector3f()
        val p0 = Vector2f()
        val a1 = Vector3f()
        val b1 = Vector3f()
        val c1 = Vector3f()
        val rot = Quaternionf()
        val p1 = Vector3f()
        repeat(numTries) {

            a0.set(random.nextFloat(), random.nextFloat())
            b0.set(random.nextFloat(), random.nextFloat())
            c0.set(random.nextFloat(), random.nextFloat())
            getRandomUVW(random, 1.5f, uvw)
            interpolateBarycentric(a0, b0, c0, uvw, p0)
            val expected = uvw.x in 0.0..1.0 && uvw.y in 0.0..1.0 && uvw.z in 0.0..1.0
            val expected2 = p0.isInsideTriangleBaseline(a0, b0, c0)

            val z0 = random.nextFloat()
            val z1 = random.nextFloat()
            rot.rotationYXZ(
                random.nextFloat() * TAUf,
                random.nextFloat() * TAUf,
                random.nextFloat() * TAUf
            )
            a1.set(a0, z0).rotate(rot)
            b1.set(b0, z0).rotate(rot)
            c1.set(c0, z0).rotate(rot)
            p1.set(p0, z1).rotate(rot)

            val actual = p1.isInsidePolygon(listOf(a1, b1, c1))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }

    @Test
    fun testInsidePolygon3d() {
        val random = Random(1234)
        val a0 = Vector2d()
        val b0 = Vector2d()
        val c0 = Vector2d()
        val uvw = Vector3d()
        val p0 = Vector2d()
        val a1 = Vector3d()
        val b1 = Vector3d()
        val c1 = Vector3d()
        val rot = Quaterniond()
        val p1 = Vector3d()
        repeat(numTries) {

            a0.set(random.nextDouble(), random.nextDouble())
            b0.set(random.nextDouble(), random.nextDouble())
            c0.set(random.nextDouble(), random.nextDouble())
            getRandomUVW(random, 1.5, uvw)
            interpolateBarycentric(a0, b0, c0, uvw, p0)
            val expected = uvw.x in 0.0..1.0 && uvw.y in 0.0..1.0 && uvw.z in 0.0..1.0
            val expected2 = p0.isInsideTriangleBaseline(a0, b0, c0)

            val z0 = random.nextDouble()
            val z1 = random.nextDouble()
            rot.rotationYXZ(
                random.nextDouble() * TAU,
                random.nextDouble() * TAU,
                random.nextDouble() * TAU
            )
            a1.set(a0, z0).rotate(rot)
            b1.set(b0, z0).rotate(rot)
            c1.set(c0, z0).rotate(rot)
            p1.set(p0, z1).rotate(rot)

            val actual = p1.isInsidePolygon(listOf(a1, b1, c1))
            assertEquals(expected, actual)
            assertEquals(expected, expected2)
        }
    }
}