package me.anno.gpu.texture

import me.anno.gpu.DepthMode
import me.anno.gpu.texture.TextureLib.bindWhite

class FakeWhiteTexture(override var width: Int, override var height: Int, override val samples: Int) : ITexture2D {

    override val isHDR = false
    override var depthFunc: DepthMode?
        get() = null
        set(value) {}

    override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping) = bindWhite(index)
    override fun wrapAsFramebuffer() = TextureLib.whiteTexture.wrapAsFramebuffer()
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */
    }
}