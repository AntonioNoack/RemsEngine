package me.anno.graph.visual.render.effects

import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Materials
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.deferred.DeferredLayerType.Companion.COLOR
import me.anno.gpu.deferred.DeferredLayerType.Companion.DEPTH
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.DrawSkyMode
import me.anno.graph.visual.render.scene.RenderDecalsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderGlassNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.roundDiv
import org.joml.Vector4f
import kotlin.math.log2

/**
 * AMD FSR reduces the rendering resolution to gain performance, and then reprojects and upscales that image.
 * */
class FSR2Node : RenderViewNode(
    "FSR2",
    listOf(
        "Int", "Target Width",
        "Int", "Target Height",
        "Texture", "Illuminated",
        "Texture", "Motion",
        "Texture", "Depth",
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "Depth",
        "Int", "Width",
        "Int", "Height"
    )
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
    }

    override fun executeAction() {

        var width = getIntInput(1)
        var height = getIntInput(2)
        if (width < 1 || height < 1) return

        val color = (getInput(3) as? Texture).texOrNull ?: return
        val motion = getInput(4) as? Texture ?: return
        val depth = getInput(5) as? Texture ?: return

        val scaleX = roundDiv(width, color.width)
        val scaleY = roundDiv(width, color.width)
        width -= width % scaleX
        height -= height % scaleY

        timeRendering(name, timer) {
            val fsr = renderView.fsr22
            fsr.calculate(
                color, depth.texOrNull ?: depthTexture, depth.mask1Index,
                motion.texOrNull ?: blackTexture, width, height,
                scaleX, scaleY
            )
            val view = fsr.views[RenderState.viewIndex]
            setOutput(1, Texture.texture(view.data1, 0, "xyz", COLOR))
            setOutput(2, Texture.texture(view.data1, 1, "x", DEPTH))
            setOutput(3, width)
            setOutput(4, height)
            Materials.reset()

            // unjitter Gizmos
            fsr.unjitter(RenderState.cameraMatrix)
        }
    }

    companion object {
        fun createPipeline(fraction: Float): FlowGraph {
            return QuickPipeline()
                .then1(FSR1HelperNode(), mapOf("Fraction" to fraction, "LOD Bias" to log2(fraction)))
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
                .then(FSR2Node())
                .then(GizmoNode())
                .then(FXAANode())
                .finish()
        }
    }
}