package me.anno.gpu.pipeline.transparency

import me.anno.cache.ICacheData
import me.anno.gpu.DepthMode
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

    fun draw0(pipeline: Pipeline) {
        val stages = pipeline.stages
        for (i in stages.indices) {
            val stage = stages[i]
            if (stage.blendMode == null)
                stage.bindDraw(pipeline)
        }
        draw1(pipeline)
    }

    fun draw2(pipeline: Pipeline) {
        val stages = pipeline.stages
        for (i in stages.indices) {
            val stage = stages[i]
            if (stage.blendMode != null)
                stage.drawColors(pipeline)
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

    abstract fun draw1(pipeline: Pipeline)
}