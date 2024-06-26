package me.anno.gpu.texture

import me.anno.gpu.DepthMode
import me.anno.gpu.texture.TextureLib.bindWhite
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.Warning.unused

class FakeWhiteTexture(override var width: Int, override var height: Int, override val samples: Int) : ITexture2D {

    override val name: String
        get() = "FakeWhite"

    override val channels: Int get() = 3
    override val wasCreated: Boolean get() = true
    override val isDestroyed: Boolean get() = false
    override val filtering: Filtering get() = Filtering.TRULY_NEAREST
    override val clamping: Clamping get() = Clamping.CLAMP
    override val internalFormat: Int get() = whiteTexture.internalFormat
    override val locallyAllocated: Long get() = whiteTexture.locallyAllocated

    override val isHDR get() = false
    override var depthFunc: DepthMode?
        get() = null
        set(value) {
            unused(value)
        }

    override fun checkSession() = whiteTexture.checkSession()
    override fun bind(index: Int, filtering: Filtering, clamping: Clamping) = bindWhite(index)
    override fun destroy() { /* ignore, we don't own GFX.whiteTexture */
    }
}