package me.anno.graph.render.scene

import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getIntInput

class RenderSceneForwardNode : RenderViewNode(
    "Render Scene",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting",
        "Int", "Camera Index",
        "Boolean", "Apply ToneMapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Int", "Draw Sky", // -1 = before, 0 = don't, 1 = after
    ) + listLayers(),
    // list all available deferred layers
    listLayers()
) {

    companion object {
        fun listLayers(): List<String> {
            return listOf(
                "Texture", "Illuminated",
                "Texture", "Depth"
            )
        }
    }

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage mask
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
        setInput(8, 0) // don't bake skybox
        setInput(9, 0) // don't draw sky
    }

    // todo can we serialize a renderer somehow? make an enum from it???
    var renderer = pbrRenderer

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = getIntInput(3)
        if (width < 1 || height < 1 || samples < 1) return

        val stage = getInput(4) as PipelineStage
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int
        val applyToneMapping = getBoolInput(7)

        val framebuffer = FBStack["scene-forward",
            width, height, TargetType.Float16x4,
            samples, DepthBufferType.TEXTURE]

        GFX.check()

        // if skybox is not used, bake it anyway?
        // -> yes, the pipeline architect (^^) has to be careful
        val skyboxResolution = getIntInput(8)
        pipeline.bakeSkybox(skyboxResolution)

        val drawSky = getIntInput(9)
        val prepassColor = (getInput(10) as? Texture)?.tex
        val prepassDepth = (getInput(11) as? Texture)?.tex

        pipeline.applyToneMapping = applyToneMapping
        val depthMode = pipeline.defaultStage.depthMode
        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            defineInputs(framebuffer, prepassColor, prepassDepth)
            if (drawSky == -1) {
                pipeline.drawSky()
            }
            val stageImpl = pipeline.stages.getOrNull(stage.id)
            if (stageImpl != null && !stageImpl.isEmpty()) {
                if (stage == PipelineStage.TRANSPARENT) {
                    // todo diffuse src & emissive src are incorrect
                    //  -> because it's no longer a deferred process
                    pipeline.transparentPass.blendTransparentStage(pipeline, stageImpl)
                } else {
                    stageImpl.bindDraw(pipeline)
                }
            }
            if (drawSky == 1) {
                pipeline.drawSky()
            }
            pipeline.defaultStage.depthMode = depthMode
            GFX.check()
        }

        setOutput(1, Texture.texture(framebuffer, 0))
        setOutput(2, Texture.depth(framebuffer))
    }

    fun defineInputs(framebuffer: IFramebuffer, prepassColor: ITexture2D?, prepassDepth: ITexture2D?) {
        if (prepassDepth != null) {
            GFX.copyColorAndDepth(prepassColor ?: blackTexture, prepassDepth)
        } else {
            if (prepassColor != null && prepassColor != blackTexture) {
                framebuffer.clearDepth()
                GFX.copy(prepassColor)
            } else {
                framebuffer.clearColor(0, depth = true)
            }
        }
    }
}