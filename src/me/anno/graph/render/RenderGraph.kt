package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.graph.Node
import me.anno.graph.NodeOutput
import me.anno.graph.render.compiler.ExpressionRenderer
import me.anno.graph.render.compiler.ShaderExprNode
import me.anno.graph.render.compiler.ShaderGraphNode
import me.anno.graph.render.effects.ShapedBlurNode
import me.anno.graph.render.scene.*
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.image.ImageScale
import me.anno.io.ISaveable
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.ui.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.LOGGER
import me.anno.utils.OS.documents
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull

// todo scene processing
// stage 0:
//  scene, meshes
//  filtering & adding to pipeline
//  pipeline list with their settings
// todo stage 3:
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


    // todo node to apply lights on deferred rendering
    // is the same as SDR / HDR

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
        ) + NodeLibrary.flowNodes.nodes,
    )

    class ExprReturnNode : ReturnNode(
        listOf(
            "Vector4f", "Color",
            "Int", "Width",
            "Int", "Height",
            "Int", "Channels",
            "Int", "Samples",
            "Bool", "Apply Tone Mapping"
        )
    ), ExpressionRenderer {

        init {
            init()
        }

        override var shader: Shader? = null
        override var buffer: Framebuffer? = null
        override var typeValues: HashMap<String, TypeValue>? = null
        override fun execute(): NodeOutput? {
            throw ReturnThrowable(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            shader?.destroy()
            buffer?.destroy()
        }
    }

    // quickly create pipelines for existing RenderModes
    class QuickPipeline {

        val values = HashMap<String, NodeOutput>()
        val graph = FlowGraph()
        val start = StartNode(startArguments)

        init {
            then(start)
        }

        fun then(node: Node): QuickPipeline {
            return then(node, emptyMap(), emptyMap())
        }

        fun then(node: Node, extraOutputs: Map<String, List<String>>): QuickPipeline {
            return then(node, emptyMap(), extraOutputs)
        }

        fun then(node: Node, extraInputs: Map<String, Any?>, extraOutputs: Map<String, List<String>>): QuickPipeline {

            // connect flow, if available
            if (node.inputs?.firstOrNull()?.type == "Flow") {
                graph.nodes.lastOrNull {
                    it.outputs?.firstOrNull()?.type == "Flow"
                }?.connectTo(0, node, 0)
            }

            // set node position
            node.position.set(250.0 * graph.nodes.size, 0.0, 0.0)
            node.graph = graph
            graph.nodes.add(node)

            // connect all inputs
            val inputs = node.inputs
            if (inputs != null) for (i in inputs.indices) {
                val input = inputs[i]
                if (input.type != "Flow") {
                    val source = values[input.name]
                    if (source != null) {
                        input.connect(source)
                    } else {
                        val constant = extraInputs[input.name] ?: continue
                        node.setInput(i, constant)
                    }
                }
            }

            // register all outputs
            val outputs = node.outputs
            if (outputs != null) for (output in outputs) {
                if (output.type != "Flow") {
                    val mapping = extraOutputs[output.name]
                    if (mapping != null) {
                        for (name in mapping) {
                            values[name] = output
                        }
                    } else values[output.name] = output
                }
            }
            return this
        }

        fun render(target: DeferredLayerType): QuickPipeline {
            return then(
                RenderSceneNode(), mapOf(
                    DeferredLayerType.COLOR.name to emptyList(),
                    target.name to listOf("Color")
                )
            )
        }

        fun finish(end: ExprReturnNode = ExprReturnNode()) = then(end).graph
        fun finish(extraInputs: Map<String, Any?>, end: ExprReturnNode = ExprReturnNode()) =
            then(end, extraInputs, emptyMap()).graph

    }

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
        .then(CombineLightsNode())
        .finish(mapOf("Apply Tone Mapping" to true))

    fun draw(view: RenderView, graph: FlowGraph) {
        draw(view, view, graph)
    }

    fun draw(view: RenderView, dst: Panel, graph: FlowGraph) {

        val start = graph.nodes.firstInstanceOrNull<StartNode>()!!
        for (it in graph.nodes) {
            when (it) {
                is RenderSceneNode -> {
                    it.pipeline = view.pipeline
                    it.renderView = view
                }
                is RenderLightsNode -> {
                    it.pipeline = view.pipeline
                    it.renderView = view
                }
                is CombineLightsNode -> {
                    it.pipeline = view.pipeline
                    it.renderView = view
                }
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

    class RenderGraphEditor(val rv: RenderView, graph: FlowGraph) : GraphEditor(graph, style) {

        init {

            library = RenderGraph.library
            for (it in library.allNodes) {
                val sample = it.first
                if (sample.className !in ISaveable.objectTypeRegistry)
                    registerCustomClass(sample)
            }

            addChangeListener { _, isNodePositionChange ->
                if (!isNodePositionChange) {
                    for (node in graph.nodes) {
                        when (node) {
                            is ShaderGraphNode -> node.invalidate()
                            is ExpressionRenderer -> node.invalidate()
                            is RenderSceneNode0 -> node.invalidate()
                        }
                    }
                }
            }

        }

        override fun onUpdate() {
            super.onUpdate()
            invalidateDrawing() // for smooth rendering
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            draw(rv, this, graph as FlowGraph)
            drawNodeConnections(x0, y0, x1, y1)
            drawChildren(x0, y0, x1, y1)
        }

        override fun canDeleteNode(node: Node): Boolean {
            return node !is StartNode
        }

    }

    // todo combine light node
    // todo sample sky (tex) node
    // todo bloom node
    // todo depth of field node

    @JvmStatic
    fun main(args: Array<String>) {

        val graph = combined
        val scene = Entity()
        scene.add(MeshComponent(documents.getChild("monkey.obj")))
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