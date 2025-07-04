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
import me.anno.graph.visual.render.scene.RenderDecalsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderGlassNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import org.joml.Vector4f

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
    // todo use FBStack
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
        val color = getTextureInput(4, whiteTexture)

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
                    RenderDeferredNode(), mapOf(
                        "Stage" to PipelineStage.OPAQUE,
                        "Skybox Resolution" to 256,
                        "Draw Sky" to DrawSkyMode.AFTER_GEOMETRY
                    )
                )
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderGlassNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                // .then(FXAANode())
                .then(FSR1Node())
                .finish()
        }
    }
}