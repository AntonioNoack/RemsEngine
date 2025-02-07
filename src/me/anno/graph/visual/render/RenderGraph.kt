package me.anno.graph.visual.render

import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.node.NodeLibrary
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.compiler.ShaderExprNode
import me.anno.graph.visual.render.compiler.ShaderGraphNode
import me.anno.graph.visual.render.effects.BloomNode
import me.anno.graph.visual.render.effects.ChromaticAberrationNode
import me.anno.graph.visual.render.effects.DepthOfFieldNode
import me.anno.graph.visual.render.effects.DepthToNormalNode
import me.anno.graph.visual.render.effects.FSR1HelperNode
import me.anno.graph.visual.render.effects.FSR1Node
import me.anno.graph.visual.render.effects.FXAANode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.HeightExpFogNode
import me.anno.graph.visual.render.effects.MotionBlurNode
import me.anno.graph.visual.render.effects.NightNode
import me.anno.graph.visual.render.effects.OutlineNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.effects.ShapedBlurNode
import me.anno.graph.visual.render.effects.ToneMappingNode
import me.anno.graph.visual.render.scene.BakeSkyboxNode
import me.anno.graph.visual.render.scene.BoxCullingNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.graph.visual.render.scene.UVNode
import me.anno.graph.visual.render.scene.UViNode
import me.anno.ui.Panel
import me.anno.utils.Color.white4
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import org.apache.logging.log4j.LogManager

object RenderGraph {

    private val LOGGER = LogManager.getLogger(RenderGraph::class)

    var throwExceptions = false

    // todo highlight cpu/gpu computations (silver/gold)

    val library = NodeLibrary(
        listOf(
            { BoxCullingNode() },
            { RenderDeferredNode() },
            { RenderReturnNode() },
            { ShaderExprNode() },
            { ShaderGraphNode() },
            { UVNode() },
            { UViNode() },
            { ShapedBlurNode() },
            { RenderLightsNode() },
            { GizmoNode() },
            { SSAONode() },
            { SSRNode() },
            { BloomNode() },
            { DepthOfFieldNode() },
            { DepthToNormalNode() },
            { ToneMappingNode() },
            { FSR1HelperNode() },
            { FSR1Node() },
            { MotionBlurNode() },
            { NightNode() },
            { HeightExpFogNode() },
            { OutlineNode() },
            { FXAANode() },
            { ChromaticAberrationNode() },
            // { GodRaysNode() }, // not yet ready

        ) + NodeLibrary.flowNodes.nodes,
    )

    val startArguments = listOf(
        "Int", "Width",
        "Int", "Height",
    )

    fun draw(rv: RenderView, dst: Panel, graph: FlowGraph) {
        val startNode = findStartNode(graph) ?: return
        initGraphState(graph, rv, startNode)
        val result = executeGraph(graph, startNode)
        if (result != null) {
            drawResult(result, rv, dst)
        } else {
            LOGGER.warn("Missing end")
        }
        Material.lodBias = 0f // reset lod bias
        Pipeline.currentInstance = null // reset pipeline
    }

    private fun findStartNode(graph: FlowGraph): StartNode? {
        return graph.nodes.firstInstanceOrNull2(StartNode::class)
    }

    private fun writeSceneIntoRenderNodes(graph: FlowGraph, renderView: RenderView) {
        val nodes = graph.nodes
        for (i in nodes.indices) {
            when (val node = nodes[i]) {
                is RenderViewNode -> {
                    node.pipeline = renderView.pipeline
                    node.renderView = renderView
                }
                is BakeSkyboxNode -> {
                    node.pipeline = renderView.pipeline
                }
            }
        }
    }

    private fun initGraphState(graph: FlowGraph, rv: RenderView, start: StartNode) {
        val renderSize = rv.renderSize
        writeSceneIntoRenderNodes(graph, rv)
        graph.invalidate()
        start.setOutput(1, renderSize.renderWidth)
        start.setOutput(2, renderSize.renderHeight)
        Material.lodBias = 0f // reset just in case
        Pipeline.currentInstance = rv.pipeline
    }

    private fun executeGraph(graph: FlowGraph, start: StartNode): RenderReturnNode? {
        return try {
            // set default blend mode to null
            renderPurely {
                // then render
                graph.execute(start) as? RenderReturnNode
            }
        } catch (e: ReturnNode.ReturnThrowable) {
            e.node as? RenderReturnNode
        } catch (e: Exception) {
            if (!OS.isWeb) Thread.sleep(100)
            if (throwExceptions) {
                throw e
            } else {
                LOGGER.warn("Error in execution", e)
                return null
            }
        }
    }

    private fun drawResult(endNode: RenderReturnNode, rv: RenderView, dst: Panel) {
        val tex = endNode.render(true)
        val texture = tex.texOrNull ?: return
        val h = dst.height
        val w = h * rv.width / rv.height
        val x = dst.x + (dst.width - w).shr(1)
        val y = dst.y
        val applyToneMapping = endNode.getBoolInput(6)
        drawTexture(x, y + h, w, -h, texture, true, white4, null, applyToneMapping)
    }

    // todo sample sky (tex) node
    // FSR 1 node
    // todo "FSR 2 node" (a little more complicated, needs shaker)
    // todo TAA node (needs shaker, too)
    // todo when we have a Vulkan/DX11/DX12 backend, add DLSS nodes
    //  https://developer.nvidia.com/rtx/dlss/get-started#sdk-requirements

    // todo film grain node? film stripes node?

}