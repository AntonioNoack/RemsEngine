package me.anno.engine.ui.render

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderView.Companion.MAX_FORWARD_LIGHTS
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredRenderer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.pipeline.Sorting

/**
 * buffers for legacy RenderView-rendering
 * */
class RenderBuffers : ICacheData {

    val deferred = DeferredRenderer.deferredSettings!!

    val baseNBuffer1 = deferred.createBaseBuffer("DeferredBuffers-main", 1)
    val baseSameDepth1 = baseNBuffer1.attachFramebufferToDepth("baseSD1", 1, false)
    val depthType get() = if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
    val base1Buffer = Framebuffer("base1", 1, 1, 1, 1, false, depthType)

    val light1Buffer = base1Buffer.attachFramebufferToDepth("light1", listOf(TargetType.Float16x4))
    val lightNBuffer1 = baseNBuffer1.attachFramebufferToDepth("lightN1", listOf(TargetType.Float16x4))

    val stage0 = PipelineStageImpl(
        "default",
        Sorting.NO_SORTING,
        MAX_FORWARD_LIGHTS,
        null,
        DepthMode.CLOSE,
        true,
        CullMode.FRONT,
        pbrModelShader
    )

    val pipeline = Pipeline(deferred)
    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    override fun destroy() {
        light1Buffer.destroy()
        lightNBuffer1.destroy()
        baseSameDepth1.destroy()
        base1Buffer.destroy()
        baseNBuffer1.destroy()
        pipeline.destroy()
    }

}