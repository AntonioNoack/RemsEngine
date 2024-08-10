package me.anno.graph.visual.render.effects

import me.anno.Time
import me.anno.config.ConfigRef
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.control.FixedControlFlowNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderDecalsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderGlassNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.maths.Maths.posMod
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f

class FrameGenInitNode : FixedControlFlowNode(
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
        val frameIndex get() = Time.frameIndex

        fun skipThisFrame(): Boolean {
            return posMod(frameIndex, interFrames) > 0
        }

        fun isLastFrame(): Boolean {
            val interFrames = interFrames
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