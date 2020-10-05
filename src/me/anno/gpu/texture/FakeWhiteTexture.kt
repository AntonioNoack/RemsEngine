package me.anno.gpu.texture

import me.anno.gpu.TextureLib.whiteTexture

class FakeWhiteTexture(override var w: Int, override var h: Int): ITexture2D {
    override fun bind(nearest: NearestMode, clampMode: ClampMode) = whiteTexture.bind(nearest, whiteTexture.clampMode)
    override fun bind(index: Int, nearest: NearestMode, clampMode: ClampMode) = whiteTexture.bind(index, whiteTexture.nearest, whiteTexture.clampMode)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */ }
}