package me.anno.tests.gfx

import me.anno.gpu.drawing.Perspective
import me.anno.maths.Maths
import org.joml.Matrix4f

fun main() {
    val m = Matrix4f()
    Perspective.setPerspective(m, Maths.PIf / 2f, 2.8675f, 1e-10f, 1e10f, 0f, 0f)
    println(m)
}