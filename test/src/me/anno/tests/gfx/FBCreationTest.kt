package me.anno.tests.gfx

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import org.junit.jupiter.api.Test

class FBCreationTest {
    @Test
    fun testTextureDepthWithMSAA() {
        HiddenOpenGLContext.createOpenGL()
        Framebuffer("test", 253, 101, 8, 1, true, DepthBufferType.TEXTURE)
            .clearDepth()
    }

    @Test
    fun testInternalDepthWithMSAA() {
        // fixed sample locations wasn't set, which caused a crash
        HiddenOpenGLContext.createOpenGL()
        Framebuffer("", 256, 256, 8, arrayOf(TargetType.UInt8x4), DepthBufferType.INTERNAL)
            .create()
    }
}