package me.anno.gpu.texture

import me.anno.gpu.texture.TextureLib.bindWhite

class FakeWhiteTexture(override var w: Int, override var h: Int) : ITexture2D {
    override val isHDR = false
    // override fun bind(nearest: GPUFiltering, clamping: Clamping) = whiteTexture.bind(nearest, whiteTexture.clamping)
    override fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping) = bindWhite(index)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */
    }
}