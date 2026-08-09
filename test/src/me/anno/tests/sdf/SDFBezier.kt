package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.TAUf
import me.anno.sdf.shapes.SDFBezierCurve
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val curve = SDFBezierCurve()
    val n = 48 // really expensive!!!
    curve.points.clear()
    for (i in 0 until n) {
        val angle = 5f * i * TAUf / n
        curve.points.add(Vector4f(cos(angle), sin(angle), i * 0.1f, 0.1f))
    }
    testSceneWithUI("SDFBezierCurve", curve)
}