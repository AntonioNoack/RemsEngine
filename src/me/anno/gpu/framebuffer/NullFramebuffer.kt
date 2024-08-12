package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import org.lwjgl.opengl.GL46C

/**
 * standard, presentation framebuffer
 * */
object NullFramebuffer : IFramebuffer {

    override val name = "null"
    override val pointer = 0

    override val width get() = (GFX.activeWindow ?: GFX.someWindow).width
    override val height get() = (GFX.activeWindow ?: GFX.someWindow).height
    override val samples = 1
    override val numTextures = 1

    override fun ensure() {}

    override fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int) {
        // more isn't supported
    }

    override fun bindDirectly() {
        Framebuffer.bindFramebuffer(GL46C.GL_FRAMEBUFFER, 0)
        Frame.lastPtr = 0
    }

    override fun bindDirectly(w: Int, h: Int) {
        bindDirectly()
    }

    override fun destroy() {} // cannot really be destroyed

    override fun getTargetType(slot: Int) = TargetType.UInt8x4 // really?

    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        throw UnsupportedOperationException()
    }

    override fun checkSession() {}

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        throw UnsupportedOperationException()
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        throw UnsupportedOperationException()
    }

    override fun getTextureI(index: Int): ITexture2D {
        throw UnsupportedOperationException()
    }

    override val depthTexture
        get() = throw UnsupportedOperationException()
    override val depthMask: Int
        get() = 0

}