package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
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
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.LOGGER
import me.anno.utils.OS.documents
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
            { FSR1Node() },
            { MotionBlurNode() },
            { OutlineNode() },
            { FXAANode() },
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
        .then(GizmoNode(), mapOf("Samples" to 8), mapOf("Illuminated" to listOf("Color")))
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
        .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
        .then(FXAANode())
        .then(GizmoNode(), mapOf("Samples" to 8), mapOf("Illuminated" to listOf("Color")))
        .finish()

    val outlined = QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then1(CombineLightsNode(), mapOf("Apply Tone Mapping" to true))
        .then1(OutlineNode(), mapOf("Diffuse" to null, "Depth" to null, "Color" to Vector4f(1f, 0.31f, 0.51f, 1f)))
        .then(FXAANode(), mapOf("Illuminated" to listOf("Color")))
        .finish()

    fun draw(view: RenderView, graph: FlowGraph) {
        draw(view, view, graph)
    }

    fun draw(view: RenderView, dst: Panel, graph: FlowGraph) {

        val start = graph.nodes.firstInstanceOrNull<StartNode>()!!
        for (it in graph.nodes) {
            if (it is RenderSceneNode0) {
                it.pipeline = view.pipeline
                it.renderView = view
            }
        }

        graph.invalidate()

        start.setOutput(dst.w, 1)
        start.setOutput(dst.h, 2)

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
        val (w, h) = ImageScale.scaleMax(texture.w, texture.h, dst.w, dst.h)
        val x = dst.x + (dst.w - w) / 2
        val y = dst.y + (dst.h - h) / 2
        val applyToneMapping = endNode.getInput(6) == true
        drawTexture(x, y + h, w, -h, texture, false, -1, null, applyToneMapping)

    }


    // todo sample sky (tex) node
    // FSR 1 node
    // todo "FSR 2 node" (a little more complicated, needs shaker)
    // todo TAA node (needs shaker, too)
    // todo when we have a Vulkan backend, add DLSS nodes
    //  https://developer.nvidia.com/rtx/dlss/get-started#sdk-requirements

    // todo vignette node
    // todo chromatic aberration node
    // todo film grain node? film stripes node?

    // todo: physics demo: dominos like https://www.youtube.com/watch?v=YZxky260O-4

    @JvmStatic
    fun main(args: Array<String>) {

        val graph = outlined
        val scene = Entity()
        scene.add(MeshComponent(documents.getChild("metal-roughness.glb")))
        scene.add(SkyBox())
        testUI {

            EditorState.prefabSource = scene.ref

            val sv = SceneView(EditorState, PlayMode.EDITING, style)
            val rv = sv.renderer
            rv.position.set(0.0, 0.0, -5.0)
            rv.updateEditorCameraTransform()

            val list = CustomList(false, style)
            list.add(sv, 1f)
            list.add(RenderGraphEditor(rv, graph), 1f)
            list
        }
    }

}