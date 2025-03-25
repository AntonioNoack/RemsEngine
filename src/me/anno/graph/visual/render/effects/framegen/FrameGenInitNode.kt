package me.anno.graph.visual.render.effects.framegen

import me.anno.Time
import me.anno.config.ConfigRef
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.effects.BloomNode
import me.anno.graph.visual.render.effects.FXAANode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.OutlineEffectNode
import me.anno.graph.visual.render.effects.OutlineEffectSelectNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.effects.UnditherNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderDecalsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderGlassNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.maths.Maths.max
import me.anno.maths.Maths.posMod
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f

class FrameGenInitNode : FlowGraphNode(
    "FrameGenInit",
    listOf("Flow", beforeName), listOf(
        "Flow", afterName,
        "Flow", "Shortcut"
    )
) {

    override fun execute(): NodeOutput {
        val skip = skipThisFrame()
        return getNodeOutput(skip.toInt())
    }

    fun skipThisFrame(): Boolean {
        return Companion.skipThisFrame()
    }

    companion object {

        var interFrames by ConfigRef("gpu.frameGen.intermediateFrames", 1)
        val totalFrames get() = 1 + max(0, interFrames)
        val frameIndex get() = Time.frameIndex

        fun skipThisFrame(): Boolean {
            return posMod(frameIndex, totalFrames) > 0
        }

        fun isLastFrame(): Boolean {
            val interFrames = totalFrames
            return posMod(frameIndex, interFrames) == interFrames - 1
        }

        fun createPipeline(output: Node): FlowGraph {
            // standard node setup except for init and output
            val init = FrameGenInitNode()
            val graph = QuickPipeline()
                .then(init)
                .then1(RenderDeferredNode(), opaqueNodeSettings)
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
                .then(UnditherNode())
                .then(FXAANode())
                .then(output)
                .finish()
            init.connectTo(1, output, 0)
            return graph
        }
    }
}