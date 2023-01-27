package me.anno.tests.geometry

import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.utils.types.Triangles
import org.joml.Vector3d
import org.joml.Vector3f

fun main() {
    testSubCrossDot()
    testTriangleTest()
}

fun testTriangleTest() {
    val a = Vector3d()
    val b = Vector3d(0.4, 1.0, 0.0)
    val c = Vector3d(1.0, 0.0, 0.0)
    val origin = Vector3d(0.5, 0.5, -1.1)
    val size = 256
    ImageWriter.writeImageInt(size, size, false, "triangle", 512) { x, y, _ ->
        val direction = Vector3d((x - size * 0.5) / size, -(y - size * 0.5) / size, 1.0)
        if (Triangles.rayTriangleIntersection(
                origin, direction, a, b, c,
                1e3, Vector3d(), Vector3d()
            ).isFinite()
        ) 0 else -1
    }
}

fun testSubCrossDot() {
    val a = Vector3f(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
    val b = Vector3f(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
    val c = Vector3f(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
    val n = Vector3f(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
    println((b - a).cross(c - a).dot(n))
    println(Triangles.subCrossDot(a, b, c, n))
}
