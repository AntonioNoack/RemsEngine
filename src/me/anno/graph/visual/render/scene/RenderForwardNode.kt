package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.Renderers.pbrRenderer
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
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.maths.Maths.clamp

class RenderForwardNode : RenderViewNode(
    "RenderSceneForward",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Boolean", "Apply ToneMapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Enum<me.anno.graph.visual.render.scene.DrawSkyMode>", "Draw Sky",
    ) + listLayers(),
    // list all available deferred layers
    listLayers()
) {

    companion object {
        fun listLayers(): List<String> {
            return listOf("Texture", "Illuminated", "Texture", "Depth")
        }
    }

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage
        setInput(5, false) // don't apply tonemapping
        setInput(6, 0) // don't bake skybox
        setInput(7, DrawSkyMode.DONT_DRAW_SKY)
    }

    var renderer = pbrRenderer

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        val stage = getInput(4) as PipelineStage
        val applyToneMapping = getBoolInput(5)
        val skyboxResolution = getIntInput(6)
        val drawSky = getInput(7) as DrawSkyMode
        val stageImpl = pipeline.stages.getOrNull(stage.id)

        val skipSky = drawSky == DrawSkyMode.DONT_DRAW_SKY || skyboxResolution <= 0
        val skipGeometry = stageImpl == null || stageImpl.isEmpty()
        if (skipSky && skipGeometry) {
            // if there is nothing to render, redirect inputs/defaults to outputs
            setOutput(1, getInput(8) ?: Texture(blackTexture))
            setOutput(2, getInput(9) ?: Texture(depthTexture))
            return
        }
        timeRendering("$name-$stage", timer) {

            val framebuffer = FBStack["scene-forward",
                width, height, TargetType.Float16x4,
                samples, DepthBufferType.TEXTURE]

            // if skybox is not used, bake it anyway?
            // -> yes, the pipeline architect (^^) has to be careful
            pipeline.bakeSkybox(skyboxResolution)

            val prepassColor = (getInput(8) as? Texture).texOrNull
            val prepassDepth = getInput(9) as? Texture

            pipeline.applyToneMapping = applyToneMapping
            val depthMode = pipeline.defaultStage.depthMode
            GFXState.useFrame(width, height, true, framebuffer, renderer) {
                defineInputs(framebuffer, prepassColor, prepassDepth.texOrNull, prepassDepth.mask1Index)
                if (drawSky == DrawSkyMode.BEFORE_GEOMETRY) {
                    pipeline.drawSky()
                }

                if (stageImpl != null && !stageImpl.isEmpty()) {
                    stageImpl.bindDraw(pipeline)
                }
                if (drawSky == DrawSkyMode.AFTER_GEOMETRY) {
                    pipeline.drawSky()
                }
                pipeline.defaultStage.depthMode = depthMode
                GFX.check()
            }

            setOutput(1, Texture.texture(framebuffer, 0))
            setOutput(2, Texture.depth(framebuffer))
        }
    }

    fun defineInputs(
        framebuffer: IFramebuffer, prepassColor: ITexture2D?,
        prepassDepth: ITexture2D?, prepassDepthM: Int
    ) {
        if (prepassDepth != null && prepassDepth.isCreated()) {
            GFX.copyColorAndDepth(prepassColor ?: blackTexture, prepassDepth, prepassDepthM)
        } else if (prepassColor != null && prepassColor != blackTexture) {
            framebuffer.clearDepth()
            GFX.copy(prepassColor)
        } else {
            framebuffer.clearColor(0, depth = true)
        }
    }
}