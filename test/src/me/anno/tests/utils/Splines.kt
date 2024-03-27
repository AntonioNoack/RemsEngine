package me.anno.tests.utils

import me.anno.ecs.components.mesh.spline.Splines
import me.anno.image.ImageWriter
import org.joml.Vector2f
import org.joml.Vector3f

fun main() {
    val c0 = Splines.generateCurve(0f, 1.57f, 15).map { Vector3f(it.x, 0f, it.y) }
    val c1 = Splines.generateCurve(0f, 1.57f, 5).map { Vector3f(it.x, 0f, it.y).mul(0.5f) }
    val surf = Splines.generateSurface(c0, c1)
    ImageWriter.writeTriangles(512, "surface.png", (c0 + c1).map { Vector2f(it.x, it.z) }, surf)
}