package me.anno.graph.visual.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.drawing.DrawTextures.drawTexture
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
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderSceneDeferredNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.graph.visual.render.scene.UVNode
import me.anno.graph.visual.render.scene.UViNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.node.NodeLibrary
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.image.ImageScale
import me.anno.ui.Panel
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import org.apache.logging.log4j.LogManager

// stage 0:
//  scene, meshes
//  filtering & adding to pipeline
//  pipeline list with their settings
// stage 3:
//  applying effects & calculations
// todo stage 4:
//  primary & debug outputs; name -> enum in RenderView
// data type is RGBA or Texture2D or sth like this

// todo create collection of render graphs for debugging

object RenderGraph {

    private val LOGGER = LogManager.getLogger(RenderGraph::class)

    var throwExceptions = false

    // todo inputs / settings:
    //  - stage index/id
    //  - depth sorting
    //  - number of lights if forward
    //  - clear sky / use other background (e.g. transparent uses backside pass)
    //  - camera override / camera index
    //  - relative size (or maybe even more flexible by width x height)

    // todo highlight cpu/gpu computations (silver/gold)

    val library = NodeLibrary(
        listOf(
            { RenderSceneDeferredNode() },
            { ExprReturnNode() },
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

    fun draw(view: RenderView, dst: Panel, graph: FlowGraph) {
        val startNode = findStartNode(graph) ?: return
        initGraphState(graph, dst, view, startNode)
        val result = executeGraph(graph, startNode)
        if (result != null) {
            drawResult(result, dst)
        } else {
            LOGGER.warn("Missing end")
        }
    }

    private fun findStartNode(graph: FlowGraph): StartNode? {
        val nodes = graph.nodes
        return nodes.firstInstanceOrNull<StartNode>()
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

    private fun initGraphState(graph: FlowGraph, dst: Panel, renderView: RenderView, start: StartNode) {
        writeSceneIntoRenderNodes(graph, renderView)
        graph.invalidate()
        start.setOutput(1, dst.width)
        start.setOutput(2, dst.height)
    }

    private fun executeGraph(graph: FlowGraph, start: StartNode): ExprReturnNode? {
        return try {
            // set default blend mode to null
            renderPurely {
                // then render
                graph.execute(start) as? ExprReturnNode
            }
        } catch (e: ReturnNode.ReturnThrowable) {
            e.node as? ExprReturnNode
        } catch (e: Exception) {
            if (throwExceptions) {
                throw e
            } else {
                LOGGER.warn("Error in execution", e)
                return null
            }
        }
    }

    private fun drawResult(endNode: ExprReturnNode, dst: Panel) {
        val texture = endNode.render(true)
        val (w, h) = ImageScale.scaleMax(texture.width, texture.height, dst.width, dst.height)
        val x = dst.x + (dst.width - w).shr(1)
        val y = dst.y + (dst.height - h).shr(1)
        val applyToneMapping = endNode.getInput(6) == true
        drawTexture(x, y + h, w, -h, texture, false, -1, null, applyToneMapping)
    }

    // todo sample sky (tex) node
    // FSR 1 node
    // todo "FSR 2 node" (a little more complicated, needs shaker)
    // todo TAA node (needs shaker, too)
    // todo when we have a Vulkan/DX11/DX12 backend, add DLSS nodes
    //  https://developer.nvidia.com/rtx/dlss/get-started#sdk-requirements

    // todo vignette node
    // todo film grain node? film stripes node?

}