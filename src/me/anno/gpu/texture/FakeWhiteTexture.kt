package me.anno.gpu.texture

import me.anno.gpu.texture.TextureLib.bindWhite

class FakeWhiteTexture(override var w: Int, override var h: Int) : ITexture2D {
    override val isHDR = false

    override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping) = bindWhite(index)
    override fun wrapAsFramebuffer() = TextureLib.whiteTexture.wrapAsFramebuffer()
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */
    }
}