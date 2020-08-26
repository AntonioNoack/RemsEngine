package me.anno.gpu.texture

import me.anno.gpu.TextureLib.whiteTexture

class FakeWhiteTexture(override var w: Int, override var h: Int): ITexture2D {
    override fun bind(nearest: Boolean) = whiteTexture.bind(nearest)
    override fun bind(index: Int, nearest: Boolean) = whiteTexture.bind(index, nearest)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */ }
}