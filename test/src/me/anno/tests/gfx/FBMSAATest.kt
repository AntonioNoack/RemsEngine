package me.anno.tests.gfx

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext

fun main() {
    // fixed sample locations wasn't set, which caused a crash
    HiddenOpenGLContext.createOpenGL()
    Framebuffer("", 256, 256, 8, arrayOf(TargetType.UByteTarget4), DepthBufferType.INTERNAL)
        .create()
}
