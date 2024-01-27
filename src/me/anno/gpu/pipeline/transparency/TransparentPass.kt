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
        val b0 = GFXState.currentBuffer
        val tmp = if (lastK === b0) lastV!! else run {
            lastV?.destroy()
            b0.attachFramebufferToDepth("transparent", targets)
        }
        lastV = tmp
        lastK = b0
        return tmp
    }

    abstract fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl)

    fun drawTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl) {
        stage.draw(pipeline)
    }
}