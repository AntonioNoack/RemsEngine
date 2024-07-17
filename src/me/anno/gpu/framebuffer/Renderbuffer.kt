package me.anno.gpu.framebuffer

import me.anno.cache.ICacheData
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertTrue
import org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL46C.glBindRenderbuffer
import org.lwjgl.opengl.GL46C.glDeleteRenderbuffers
import org.lwjgl.opengl.GL46C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL46C.glGenRenderbuffers
import org.lwjgl.opengl.GL46C.glRenderbufferStorage
import org.lwjgl.opengl.GL46C.glRenderbufferStorageMultisample

/**
 * a renderbuffer is write-only textures on the GPU;
 * some environments may not support MSAA textures, just MSAA renderbuffers;
 * some environments may not support depth textures, just depth renderbuffers
 * */
@InternalAPI
class Renderbuffer : ICacheData {

    var pointer = 0

    fun createDepthBuffer(width: Int, height: Int, samples: Int) {
        val renderBuffer = glGenRenderbuffers()
        assertTrue(renderBuffer > 0)
        pointer = renderBuffer
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (samples > 1) glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH_COMPONENT16, width, height)
        else glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height)
    }

    fun attachToFramebuffer(bind: Boolean) {
        assertTrue(pointer > 0)
        if (bind) glBindRenderbuffer(GL_RENDERBUFFER, pointer)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, pointer)
    }

    override fun destroy() {
        if (pointer != 0) {
            glDeleteRenderbuffers(pointer)
            pointer = 0
        }
    }
}