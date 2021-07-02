package me.anno.gpu.texture

import me.anno.gpu.TextureLib.whiteTexture

class FakeWhiteTexture(override var w: Int, override var h: Int): ITexture2D {
    // override fun bind(nearest: GPUFiltering, clamping: Clamping) = whiteTexture.bind(nearest, whiteTexture.clamping)
    override fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping) = whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */ }
}