package me.anno.gpu.texture

import me.anno.gpu.GFX

class FakeWhiteTexture(override var w: Int, override var h: Int): ITexture2D {
    override fun bind(nearest: Boolean) = GFX.whiteTexture.bind(nearest)
    override fun bind(index: Int, nearest: Boolean) = GFX.whiteTexture.bind(index, nearest)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */ }
}