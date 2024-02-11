package me.anno.gpu.pipeline.transparency

import me.anno.cache.ICacheData
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl

abstract class TransparentPass : ICacheData {

    private var lastV: IFramebuffer? = null
    private var lastK: IFramebuffer? = null

    override fun destroy() {
        lastV?.destroy()
        lastV = null
        lastK = null
    }

    fun getFB(targets: Array<TargetType>): IFramebuffer {
        val base = GFXState.currentBuffer
        val result = if (lastK === base) lastV!! else run {
            lastV?.destroy()
            base.attachFramebufferToDepth("transparent", targets)
        }
        lastV = result
        lastK = base
        return result
    }

    abstract fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl)
}