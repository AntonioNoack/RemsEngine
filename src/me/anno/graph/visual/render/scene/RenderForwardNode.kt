package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.maths.Maths.clamp

class RenderForwardNode : RenderViewNode(
    "RenderSceneForward",
    listOf(
        // usual rendering inputs
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Boolean", "Apply Tone Mapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Enum<me.anno.graph.visual.render.scene.DrawSkyMode>", "Draw Sky",
        // previous data
        "Texture", "Illuminated",
        "Texture", "Depth"
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "Depth"
    )
) {

    companion object {
        fun defineInputs(
            framebuffer: IFramebuffer, prepassColor: ITexture2D?,
            prepassDepth: ITexture2D?, prepassDepthM: Int
        ) {
            renderPurely {
                if (prepassDepth != null && prepassDepth.isCreated()) {
                    GizmoNode.copyColorAndDepth(prepassColor, prepassDepth, prepassDepthM)
                } else if (prepassColor != null && prepassColor != blackTexture) {
                    GizmoNode.copyColorAndDepth(prepassColor, depthTexture, 0)
                    framebuffer.clearDepth()
                } else {
                    framebuffer.clearColor(0, depth = true)
                }
            }
        }

        fun copyInputs(framebuffer: IFramebuffer, prepassColor: ITexture2D?, prepassDepth: Texture?) {
            GFXState.useFrame(framebuffer, GizmoNode.gizmoRenderer) {
                defineInputs(framebuffer, prepassColor, prepassDepth.texOrNull, prepassDepth.mask1Index)
            }
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

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)

    val stage get() = getInput(4) as PipelineStage
    val applyToneMapping get() = getBoolInput(5)
    val skyboxResolution get() = getIntInput(6)
    val drawSky get() = getInput(7) as DrawSkyMode

    val prepassColor get() = getInput(8) as? Texture
    val prepassDepth get() = getInput(9) as? Texture

    val stageImpl get() = pipeline.stages.getOrNull(stage.id)

    override fun executeAction() {
        if (width < 1 || height < 1) return

        val skipSky = drawSky == DrawSkyMode.DONT_DRAW_SKY || skyboxResolution <= 0
        val stageImpl = stageImpl
        val skipGeometry = stageImpl == null || stageImpl.isEmpty()
        if (skipSky && skipGeometry) {
            // if there is nothing to render, redirect inputs/defaults to outputs
            setOutput(1, prepassColor ?: Texture(blackTexture))
            setOutput(2, prepassDepth ?: Texture(depthTexture))
            return
        }

        timeRendering(name, timer) {
            executeRendering(stageImpl)
        }
    }

    private fun prepareFramebuffer(): IFramebuffer {
        return FBStack["scene-forward", width, height, TargetType.Float16x4, samples, DepthBufferType.TEXTURE]
    }

    fun executeRendering(stageImpl: PipelineStageImpl?) {

        // if skybox is not used, bake it anyway?
        // -> yes, the pipeline architect (^^) has to be careful
        pipeline.bakeSkybox(skyboxResolution)
        pipeline.applyToneMapping = applyToneMapping

        val drawSky = drawSky
        val framebuffer = prepareFramebuffer()
        GFXState.depthMode.use(renderView.depthMode) {
            copyInputs(framebuffer, prepassColor.texOrNull, prepassDepth)
            GFXState.useFrame(framebuffer, renderer) {
                if (drawSky == DrawSkyMode.BEFORE_GEOMETRY) pipeline.drawSky()
                if (stageImpl != null && !stageImpl.isEmpty()) stageImpl.bindDraw(pipeline)
                if (drawSky == DrawSkyMode.AFTER_GEOMETRY) pipeline.drawSky()
                GFX.check()
            }
        }

        if (framebuffer.depthBufferType != DepthBufferType.NONE) {
            pipeline.prevDepthBuffer = framebuffer
        }

        setOutput(1, Texture.texture(framebuffer, 0))
        setOutput(2, Texture.depth(framebuffer))
    }
}