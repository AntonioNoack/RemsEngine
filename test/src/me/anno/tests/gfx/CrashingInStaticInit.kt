package me.anno.tests.gfx

import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.shader.effects.FSR

fun main() {
    // was crashing with a Segfault before my fix,
    // because it was trying to create a Buffer without OpenGL context
    DrawRectangles.sx
    // FSR had a similar issue: it called shader.use() for no reason
    FSR.toString()
}