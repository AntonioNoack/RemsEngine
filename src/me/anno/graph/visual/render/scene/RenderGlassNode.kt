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
        "Boolean", "IsGlass",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, true) // glass
        setInput(5, PipelineStage.GLASS) // stage
    }

    var renderer = pbrRendererNoDepth

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val useGlassPass get() = getBoolInput(4)
    val stage get() = getInput(5) as PipelineStage
    val prepassColor get() = getInput(6) as? Texture
    val prepassDepth get() = getInput(7) as? Texture

    override fun executeAction() {
        val width = width
        val height = height
        val stage = stage
        if (width > 0 && height > 0 && needsRendering(stage)) {
            render(width, height, stage)
        } else setOutput(1, prepassColor)
    }

    private fun render(width: Int, height: Int, stage: PipelineStage) {
        timeRendering("$name-$stage", timer) {

            val framebuffer = FBStack["scene-glass",
                width, height, TargetType.Float16x4,
                samples, DepthBufferType.INTERNAL]

            val prepassColor0 = prepassColor
            val prepassColor1 = if (samples > 1) prepassColor0.texMSOrNull else prepassColor0.texOrNull
            val prepassColor2 = prepassColor1 ?: blackTexture

            val prepassDepth0 = prepassDepth
            val prepassDepth1 = if (samples > 1) prepassDepth0.texMSOrNull else prepassDepth0.texOrNull

            GFXState.useFrame(width, height, true, framebuffer, renderer) {
                defineInputs(framebuffer, prepassColor2, prepassDepth1, prepassDepth0.mask1Index)
                val stageImpl = pipeline.stages.getOrNull(stage.id)
                if (stageImpl != null && !stageImpl.isEmpty()) {
                    val pass = if (useGlassPass) pipeline.glassPass else pipeline.alphaBlendPass
                    pass.blendTransparentStage(pipeline, stageImpl, prepassColor2)
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
            Blitting.copyColor(prepassColor, true)
        } else {
            framebuffer.clearColor(0, depth = true)
        }
    }
}