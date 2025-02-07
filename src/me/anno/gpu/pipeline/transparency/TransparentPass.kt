package me.anno.gpu.pipeline.transparency

import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.texture.ITexture2D

abstract class TransparentPass : AttachedDepthPass() {
    abstract fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl, colorInput: ITexture2D)
}