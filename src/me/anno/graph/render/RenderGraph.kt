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
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.graph.Node
import me.anno.graph.NodeOutput
import me.anno.graph.render.compiler.ExpressionRenderer
import me.anno.graph.render.compiler.ShaderExprNode
import me.anno.graph.render.compiler.ShaderGraphNode
import me.anno.graph.render.scene.SceneNode
import me.anno.graph.render.scene.UVNode
import me.anno.graph.render.scene.UViNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.NodePositionOptimization.calculateNodePositions
import me.anno.image.ImageScale
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.LOGGER

// todo scene processing
// todo stage 0:
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

    val library = NodeLibrary(
        listOf(
            { SceneNode() },
            { ExprReturnNode() },
            { ShaderExprNode() },
            { ShaderGraphNode() },
            { UVNode() },
            { UViNode() },
        ) + NodeLibrary.flowNodes.nodes,
    )

    class ExprReturnNode : ReturnNode(
        listOf(
            "Vector4f", "Color",
            "Int", "Width",
            "Int", "Height",
            "Int", "Channels",
            "Int", "Samples"
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

    @JvmStatic
    fun main(args: Array<String>) {

        val graph = FlowGraph()
        var scene: Entity? = null

        val start = StartNode(
            listOf(
                "Int", "Width",
                "Int", "Height",
            )
        )

        fun draw(view: RenderView, dst: Panel) {

            for (it in graph.nodes) {
                if (it is SceneNode) {
                    it.pipeline = view.pipeline
                    it.renderView = view
                }
            }

            // calculate, which scenes/meshes we need

            val scene1 = scene ?: view.getWorld()
            if (scene1 == null) {
                LOGGER.warn("Missing scene")
                return
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

            val texture = endNode.render()
            val (w, h) = ImageScale.scaleMax(texture.w, texture.h, dst.w, dst.h)
            val x = dst.x + (dst.w - w) / 2
            val y = dst.y + (dst.h - h) / 2
            drawTexture(x, y + h, w, -h, texture, false, -1)
        }

        val sceneNode = SceneNode()
        val retNode = ExprReturnNode()
        graph.add(start)
        graph.add(sceneNode)
        graph.add(retNode)
        // exec
        start.connectTo(sceneNode)
        // size
        start.connectTo(1, retNode, 2)
        start.connectTo(2, retNode, 3)
        sceneNode.connectTo(retNode)
        start.connectTo(1, sceneNode, 1)
        start.connectTo(2, sceneNode, 2)
        // image
        sceneNode.connectTo(2, retNode, 1)

        scene = Entity()
        val cube = MeshComponent(flatCube.front)
        scene.add(cube)
        scene.add(SkyBox())
        testUI {

            EditorState.prefabSource = scene.ref

            val sv = SceneView(EditorState, PlayMode.EDITING, style)
            val rv = sv.renderer
            rv.position.set(0.0, 0.0, -5.0)
            rv.updateEditorCameraTransform()

            val list = CustomList(false, style)
            list.add(sv, 1f)
            list.add(object : GraphEditor(graph, style) {

                init {

                    // calculate initial node positions
                    calculateSize(500, 500)
                    setPosSize(0, 0, 500, 500) // calculate connector offsets
                    calculateNodePositions(graph.nodes)

                    // todo center graph panel on nodes by calculating their mid point
                    library = RenderGraph.library

                    // todo lighting pass node

                    addChangeListener { _, isNodePositionChange ->
                        if (!isNodePositionChange) {
                            for (n in graph.nodes) {
                                when (n) {
                                    is ShaderGraphNode -> {
                                        n.shader?.destroy()
                                        n.shader = null
                                    }
                                    is ExpressionRenderer -> {
                                        n.shader?.destroy()
                                        n.shader = null
                                    }
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
                    drawBackground(x0, y0, x1, y1)
                    drawGrid(x0, y0, x1, y1)
                    draw(rv, this)
                    drawNodeConnections(x0, y0, x1, y1)
                    drawChildren(x0, y0, x1, y1)
                }

                override fun canDeleteNode(node: Node): Boolean {
                    return node !is StartNode
                }

            }, 1f)
            list
        }
    }

}