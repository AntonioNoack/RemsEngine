package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.effects.FSR
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.DrawSkyMode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderForwardNode
import me.anno.graph.visual.render.scene.RenderLightsNode

/**
 * AMD FSR reduces the rendering resolution to gain performance, and then upscales that image.
 * */
class FSR1Node : TimedRenderingNode(
    "FSR1",
    listOf(
        "Int", "Target Width",
        "Int", "Target Height",
        "Float", "Sharpness",
        "Texture", "Illuminated",
    ), listOf(
        "Texture", "Illuminated",
        "Int", "Width",
        "Int", "Height"
    )
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1f) // sharpness
    }

    // hdr? upscaling, so not really necessary
    private val f0 = Framebuffer("fsr1-0", 1, 1, TargetType.UInt8x4)
    private val f1 = Framebuffer("fsr1-1", 1, 1, TargetType.UInt8x4)

    override fun destroy() {
        super.destroy()
        f0.destroy()
        f1.destroy()
    }

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        if (width < 1 || height < 1) return

        val sharpness = getFloatInput(3)
        val color = (getInput(4) as? Texture)?.texOrNull ?: whiteTexture

        timeRendering(name, timer) {
            useFrame(width, height, true, f0, copyRenderer) {
                FSR.upscale(color, 0, 0, width, height, flipY = true, applyToneMapping = false, withAlpha = false)
            }

            if (sharpness > 0f) {
                useFrame(width, height, true, f1, copyRenderer) {
                    FSR.sharpen(f0.getTexture0(), sharpness, 0, 0, width, height, flipY = true)
                }
                setOutput(1, Texture.texture(f1, 0))
            } else {
                setOutput(1, Texture.texture(f0, 0))
            }

            setOutput(2, width)
            setOutput(3, height)
        }
    }

    companion object {
        fun createPipeline(fraction: Float): FlowGraph {
            return QuickPipeline()
                .then1(FSR1HelperNode(), mapOf("Fraction" to fraction))
                .then1(
                    RenderDeferredNode(),
                    mapOf(
                        "Stage" to PipelineStage.OPAQUE,
                        "Skybox Resolution" to 256,
                        "Draw Sky" to DrawSkyMode.AFTER_GEOMETRY
                    )
                )
                .then1(RenderDeferredNode(), mapOf("Stage" to PipelineStage.DECAL))
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderForwardNode(), mapOf("Stage" to PipelineStage.TRANSPARENT))
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                // todo scale depth if needed in gizmo node?
                .then(GizmoNode()) // gizmo node depends on 1:1 depth scale, so we cannot do FSR before it
                .then(FSR1Node())
                .finish()
        }
    }
}