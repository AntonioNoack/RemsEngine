package me.anno.tests.shader

import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.ComputeShader

fun main() {
    HiddenOpenGLContext.createOpenGL()
    println(ComputeShader.stats.joinToString())
}
