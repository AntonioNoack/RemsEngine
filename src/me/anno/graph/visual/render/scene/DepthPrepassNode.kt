package me.anno.graph.visual.render.scene

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths.clamp

class DepthPrepassNode : RenderViewNode(
    "Depth Prepass",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
    ),
    listOf("Texture", "Depth")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage
    }

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val stage get() = getInput(4) as PipelineStage

    override fun executeAction() {
        val width = width
        val height = height
        val samples = samples
        if (width < 1 || height < 1) return
        val framebuffer = FBStack["prepass", width, height, 0, false, samples, DepthBufferType.TEXTURE]
        timeRendering("$name-$stage", timer) {
            // bind the renderMode, so we know what to clear depth to
            GFXState.depthMode.use(renderView.depthMode) {
                framebuffer.clearColor(0, depth = true)
            }
            val renderer = if (GFX.supportsDepthTextures) Renderer.nothingRenderer
            else Renderer.depthRenderer // is this needed?
            GFXState.useFrame(width, height, true, framebuffer, renderer) {
                pipeline.stages.getOrNull(stage.id)?.bindDraw(pipeline)
            }
            setOutput(1, Texture.depth(framebuffer))
        }
    }
}