package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.viewportHeight
import me.anno.gpu.GFX.viewportWidth
import me.anno.gpu.GFX.viewportX
import me.anno.gpu.GFX.viewportY
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import org.lwjgl.opengl.GL46C

/**
 * standard, presentation framebuffer
 * */
object NullFramebuffer : IFramebuffer {

    override var name = "null"
    override val pointer = INVALID_POINTER

    override val width get() = (GFX.activeWindow ?: GFX.someWindow).width
    override val height get() = (GFX.activeWindow ?: GFX.someWindow).height
    override val samples = 1
    override val numTextures = 1
    override var isSRGBMask = 1
    override val depthBufferType: DepthBufferType
        get() = DepthBufferType.NONE // not really readable

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


    @JvmStatic
    fun setFrameNullSize(window: OSWindow) {
        setFrameNullSize(window.width, window.height)
    }

    @JvmStatic
    fun setFrameNullSize(width: Int, height: Int) {

        // this should be the state for the default framebuffer
        GFXState.xs[0] = 0
        GFXState.ys[0] = 0
        GFXState.ws[0] = width
        GFXState.hs[0] = height
        GFXState.changeSizes[0] = false

        Frame.invalidate()
        viewportX = 0
        viewportY = 0
        viewportWidth = width
        viewportHeight = height
    }
}