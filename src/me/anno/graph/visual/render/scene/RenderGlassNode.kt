package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.Renderers.pbrRendererNoDepth
import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texMSOrNull
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.maths.Maths.clamp

class RenderGlassNode : RenderViewNode(
    "RenderSceneGlass",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Texture", "Illuminated",
        "Texture", "Depth"
    ),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.TRANSPARENT) // stage
    }

    var renderer = pbrRendererNoDepth

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val prepassTex get() = getInput(5) as? Texture

    override fun executeAction() {
        val width = width
        val height = height
        val stage = getInput(4) as PipelineStage
        if (width > 0 && height > 0 && needsRendering(stage)) {
            render(width, height, stage)
        } else setOutput(1, prepassTex)
    }

    private fun render(width: Int, height: Int, stage: PipelineStage) {
        timeRendering("$name-$stage", timer) {
            // val sorting = getInput(5) as Int
            // val cameraIndex = getInput(6) as Int

            val framebuffer = FBStack["scene-glass",
                width, height, TargetType.Float16x4,
                samples, DepthBufferType.INTERNAL]

            val prepassColor0 = if (samples > 1) prepassTex.texMSOrNull else prepassTex.texOrNull
            val prepassColor = prepassColor0 ?: whiteTexture
            val depthTex = getInput(6) as? Texture
            val prepassDepth = if (samples > 1) depthTex.texMSOrNull else depthTex.texOrNull

            GFXState.useFrame(width, height, true, framebuffer, renderer) {
                defineInputs(framebuffer, prepassColor, prepassDepth, depthTex.mask1Index)
                val stageImpl = pipeline.stages.getOrNull(stage.id)
                if (stageImpl != null && !stageImpl.isEmpty()) {
                    pipeline.transparentPass.blendTransparentStage(pipeline, stageImpl, prepassColor)
                }
                GFX.check()
            }

            setOutput(1, Texture.texture(framebuffer, 0))
        }
    }

    fun defineInputs(
        framebuffer: IFramebuffer, prepassColor: ITexture2D?,
        prepassDepth: ITexture2D?, prepassDepthMask: Int
    ) {
        if (prepassDepth != null && prepassDepth.isCreated()) {
            Blitting.copyColorAndDepth(
                prepassColor ?: blackTexture, prepassDepth, prepassDepthMask,
                prepassColor != null
            )
        } else if (prepassColor != null && prepassColor != blackTexture) {
            framebuffer.clearDepth()
            Blitting.copy(prepassColor, true)
        } else {
            framebuffer.clearColor(0, depth = true)
        }
    }
}