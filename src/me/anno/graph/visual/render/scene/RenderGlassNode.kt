package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.Renderers.pbrRendererNoDepth
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.maths.Maths.clamp

class RenderGlassNode : RenderViewNode(
    "RenderSceneGlass",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting",
        "Int", "Camera Index",
        "Boolean", "Apply ToneMapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Enum<me.anno.graph.visual.render.scene.DrawSkyMode>", "Draw Sky",
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
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
        setInput(8, 0) // don't bake skybox
        setInput(9, DrawSkyMode.DONT_DRAW_SKY)
    }

    var renderer = pbrRendererNoDepth

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        val stage = getInput(4) as PipelineStage
        timeRendering("$name-$stage", timer) {
            // val sorting = getInput(5) as Int
            // val cameraIndex = getInput(6) as Int
            val applyToneMapping = getBoolInput(7)

            val framebuffer = FBStack["scene-glass",
                width, height, TargetType.Float16x4,
                samples, DepthBufferType.INTERNAL]

            val prepassColor = (getInput(10) as? Texture).texOrNull ?: whiteTexture
            val depthTex = getInput(11) as? Texture
            val prepassDepth = depthTex.texOrNull

            pipeline.applyToneMapping = applyToneMapping
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
            GFX.copyColorAndDepth(prepassColor ?: blackTexture, prepassDepth, prepassDepthMask)
        } else if (prepassColor != null && prepassColor != blackTexture) {
            framebuffer.clearDepth()
            GFX.copy(prepassColor)
        } else {
            framebuffer.clearColor(0, depth = true)
        }
    }
}