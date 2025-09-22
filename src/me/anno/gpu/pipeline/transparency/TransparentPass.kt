package me.anno.gpu.pipeline.transparency

import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.texture.ITexture2D

/**
 * Renders a PipelineStage with transparency.
 *
 * Transparency can mean many things: realistic rendering? just plain alpha blending? a water pass?
 * */
abstract class TransparentPass : AttachedDepthPass() {
    abstract fun renderTransparentStage(
        pipeline: Pipeline, stage: PipelineStageImpl,
        colorInput: ITexture2D, depthInput: ITexture2D
    )
}