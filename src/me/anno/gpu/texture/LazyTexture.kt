package me.anno.gpu.texture

import me.anno.gpu.DepthMode

class LazyTexture(
    val tex: ITexture2D, val texMS: ITexture2D,
    val copyOp: Lazy<Unit>
) : ITexture2D {
    override val name: String get() = tex.name
    override val width: Int get() = texMS.width
    override val height: Int get() = texMS.height
    override val samples: Int get() = 1
    override val channels: Int get() = texMS.channels
    override val isHDR: Boolean get() = texMS.isHDR
    override val wasCreated: Boolean get() = texMS.wasCreated
    override val isDestroyed: Boolean get() = texMS.isDestroyed
    override var depthFunc: DepthMode?
        get() = tex.depthFunc
        set(value) {
            tex.depthFunc = value
        }
    override val filtering: Filtering get() = tex.filtering
    override val clamping: Clamping get() = tex.clamping
    override val locallyAllocated: Long get() = tex.locallyAllocated
    override val internalFormat: Int get() = tex.internalFormat
    override fun checkSession() {
        texMS.checkSession()
        tex.checkSession()
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        copyOp.value
        return tex.bind(index, filtering, clamping)
    }
}