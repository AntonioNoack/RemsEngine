package me.anno.gpu.pipeline.transparency

import me.anno.cache.ICacheData
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType

abstract class AttachedDepthPass : ICacheData {

    private var lastV: IFramebuffer? = null
    private var lastK: IFramebuffer? = null

    override fun destroy() {
        lastV?.destroy()
        lastV = null
        lastK = null
    }

    fun getFramebufferWithAttachedDepth(targets: List<TargetType>, base: IFramebuffer = GFXState.currentBuffer): IFramebuffer {
        val result = if (lastK === base) lastV!! else run {
            lastV?.destroy()
            base.attachFramebufferToDepth("transparent", targets)
        }
        lastV = result
        lastK = base
        return result
    }
}