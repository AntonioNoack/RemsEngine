package me.anno.gpu.pipeline.transparency

import me.anno.cache.ICacheData
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline

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

    fun drawPipeline(pipeline: Pipeline, needsClear: Boolean, drawColoredSky: Boolean) {
        if (GFXState.ditherMode.currentValue != DitherMode.DRAW_EVERYTHING) {
            drawPipelineOpaque(pipeline)
        } else {
            if (!drawColoredSky && needsClear) {
                GFXState.currentBuffer.clearColor(0)
            }
            drawOpaqueStages(pipeline)
            if (drawColoredSky) {
                pipeline.drawSky()
            }
            blendTransparentStages(pipeline)
        }
    }

    fun drawPipelineOpaque(pipeline: Pipeline) {
        // first only opaque, rest later?
        val stages = pipeline.stages
        for (i in stages.indices) {
            val stage = stages[i]
            val baseStage = if (stage.blendMode == null) stage else pipeline.defaultStage
            baseStage.bindDraw(pipeline, stage)
        }
    }

    fun drawOpaqueStages(pipeline: Pipeline) {
        val stages = pipeline.stages
        for (i in stages.indices) {
            val stage = stages[i]
            if (stage.blendMode == null) {
                stage.bindDraw(pipeline)
            }
        }
    }

    abstract fun blendTransparentStages(pipeline: Pipeline)

    fun drawTransparentStages(pipeline: Pipeline) {
        val stages = pipeline.stages
        for (i in stages.indices) {
            val stage = stages[i]
            if (stage.blendMode != null)
                stage.draw(pipeline)
        }
    }

    fun combine(draw: () -> Unit) {
        GFXState.depthMode.use(DepthMode.ALWAYS) {
            GFXState.depthMask.use(false) {
                GFXState.blendMode.use(null) {
                    draw()
                }
            }
        }
    }
}