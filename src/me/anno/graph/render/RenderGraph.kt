package me.anno.graph.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.graph.render.compiler.ShaderExprNode
import me.anno.graph.render.compiler.ShaderGraphNode
import me.anno.graph.render.effects.*
import me.anno.graph.render.scene.*
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.image.ImageScale
import me.anno.ui.Panel
import me.anno.utils.LOGGER
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import org.joml.Vector4f

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
            { RenderSceneNode() },
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
            { FSR1HelperNode() },
            { FSR1Node() },
            { MotionBlurNode() },
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

    val emissive = QuickPipeline()
        .render(DeferredLayerType.EMISSIVE)
        .finish()

    val metallic = QuickPipeline()
        .render(DeferredLayerType.METALLIC)
        .finish()

    val normal = QuickPipeline()
        .render(DeferredLayerType.NORMAL)
        .finish()

    val lights = QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode(), mapOf("Light" to listOf("Color")))
        .finish(mapOf("Apply Tone Mapping" to true))

    val combined = QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then1(CombineLightsNode(), mapOf("Apply Tone Mapping" to true))
        .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
        .finish()

    // todo sample with FSR1 node
    val combined1 = QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then(CombineLightsNode())
        .then(SSRNode())
        .then(DepthOfFieldNode())
        // .then(GodRaysNode())
        .then(ChromaticAberrationNode())
        .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
        .then(FXAANode())
        .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
        .finish()

    val outlined = QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then1(CombineLightsNode(), mapOf("Apply Tone Mapping" to true))
        .then1(OutlineNode(), mapOf("Diffuse" to null, "Depth" to null, "Color" to Vector4f(1f, 0.31f, 0.51f, 1f)))
        .then(FXAANode(), mapOf("Illuminated" to listOf("Color")))
        .finish()

    fun draw(view: RenderView, dst: Panel, graph: FlowGraph) {

        val nodes = graph.nodes
        val start = nodes.firstInstanceOrNull<StartNode>() ?: return
        for (i in nodes.indices) {
            when (val node = nodes[i]) {
                is RenderSceneNode0 -> {
                    node.pipeline = view.pipeline
                    node.renderView = view
                }
                is BakeSkyboxNode -> {
                    node.pipeline = view.pipeline
                }
            }
        }

        graph.invalidate()

        start.setOutput(1, dst.width)
        start.setOutput(2, dst.height)

        val endNode = try {
            graph.execute(start) as? ExprReturnNode
        } catch (e: ReturnNode.ReturnThrowable) {
            e.node as? ExprReturnNode
        } catch (e: Exception) {
            LOGGER.warn("Error in execution", e)
            return
        }

        if (endNode == null) {
            LOGGER.warn("Missing end")
            return
        }

        val texture = endNode.render(true)
        val (w, h) = ImageScale.scaleMax(texture.width, texture.height, dst.width, dst.height)
        val x = dst.x + (dst.width - w) / 2
        val y = dst.y + (dst.height - h) / 2
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