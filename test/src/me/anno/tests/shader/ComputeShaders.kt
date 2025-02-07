package me.anno.tests.shader

import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.ComputeShaderStats

fun main() {
    HiddenOpenGLContext.createOpenGL()
    println(ComputeShaderStats.stats)
}
