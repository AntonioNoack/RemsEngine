package me.anno.tests.shader

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ComputeShader

fun main() {
    HiddenOpenGLContext.createOpenGL()
    println(ComputeShader.stats.joinToString())
}
