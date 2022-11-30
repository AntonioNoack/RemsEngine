package me.anno.tests.gfx

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.hidden.HiddenOpenGLContext

fun main() {
    HiddenOpenGLContext.createOpenGL()
    Framebuffer("test", 253, 101, 8, 1, true, DepthBufferType.TEXTURE)
        .clearDepth()
}