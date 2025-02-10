package me.anno.tests.gfx

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.jvm.HiddenOpenGLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class FBCreationTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testTextureDepthWithMSAA() {
        HiddenOpenGLContext.createOpenGL()
        Framebuffer("test", 253, 101, 8, TargetType.Float32x4, DepthBufferType.TEXTURE)
            .clearDepth()
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testInternalDepthWithMSAA() {
        // fixed sample locations wasn't set, which caused a crash
        HiddenOpenGLContext.createOpenGL()
        Framebuffer("", 256, 256, 8, TargetType.UInt8x4, DepthBufferType.INTERNAL)
            .create()
    }
}