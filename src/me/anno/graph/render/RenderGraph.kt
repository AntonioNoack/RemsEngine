package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.render.compiler.ShaderExprNode
import me.anno.graph.render.compiler.ShaderGraphNode
import me.anno.graph.render.scene.SceneNode
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
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector4f

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

class RenderGraph {

    // todo inputs / settings:
    //  - stage index/id
    //  - depth sorting
    //  - number of lights if forward
    //  - clear sky / use other background (e.g. transparent uses backside pass)
    //  - camera override / camera index
    //  - relative size (or maybe even more flexible by width x height)


    // todo node to apply lights on deferred rendering
    // is the same as SDR / HDR

    val graph = FlowGraph()

    var scene: Entity? = null

    fun createUI(style: Style): Panel {
        val panel = GraphEditor(graph, style)
        panel.library = library
        return panel
    }

    fun draw(view: RenderView, dst: Panel) {

        for (it in graph.nodes) {
            if (it is SceneNode) {
                it.pipeline = view.pipeline
                it.renderView = view
            }
        }

        // calculate, which scenes/meshes we need

        val scene = scene ?: view.getWorld()
        if (scene == null) {
            LOGGER.warn("Missing scene")
            return
        }

        val start = graph.nodes.firstOrNull2 { it is StartNode } as? StartNode
        if (start == null) {
            LOGGER.warn("Missing start")
            return
        }

        graph.invalidate()


        val endNode = try {
            graph.execute(start) as? ReturnNode
        } catch (e: ReturnNode.ReturnException) {
            e.node
        } catch (e: Exception) {
            LOGGER.warn("Error in execution", e)
            return
        }
        if (endNode == null) {
            LOGGER.warn("Missing end")
            return
        }

        val (w, h) = ImageScale.scaleMax(view.w, view.h, dst.w, dst.h)
        val x = dst.x + (dst.w - w) / 2
        val y = dst.y + (dst.h - h) / 2
        when (val texture = endNode.values.firstOrNull2()) {
            is ITexture2D -> drawTexture(x, y + h, w, -h, texture, false, -1)
            is Texture -> texture.draw(x, y + h, w, -h)
            is Vector4f -> drawRect(x, y, w, h, texture)
            is Int -> drawRect(x, y, w, h, texture)
            else -> LOGGER.warn("Missing return value")
        }
    }

    companion object {

        val library = NodeLibrary(
            listOf(
                { SceneNode() },
                { ReturnNode() },
                { ShaderExprNode() },
                { ShaderGraphNode() },
            ) + NodeLibrary.flowNodes.nodes,
        )

        @JvmStatic
        fun main(args: Array<String>) {

            val g = RenderGraph()
            val gr = g.graph

            // todo add start to library, or make non-deletable
            // todo add return to library

            val start = StartNode()
            val sceneNode = SceneNode()
            val retNode = ReturnNode(listOf("Texture", "Result"))
            gr.add(start)
            gr.add(sceneNode)
            gr.add(retNode)
            start.connectTo(sceneNode)
            sceneNode.connectTo(retNode)
            sceneNode.connectTo(2, retNode, 1)

            val scene = Entity()
            val cube = MeshComponent(flatCube.front.ref)
            scene.add(cube)
            scene.add(SkyBox())
            g.scene = scene
            testUI {

                EditorState.prefabSource = scene.ref

                val sv = SceneView(EditorState, PlayMode.EDITING, style)
                val rv = sv.renderer
                rv.position.set(0.0, 0.0, -5.0)
                rv.updateEditorCameraTransform()

                val list = CustomList(false, style)
                list.add(sv, 1f)
                list.add(object : GraphEditor(gr, style) {

                    init {

                        // calculate initial node positions
                        calculateSize(500, 500)
                        setPosSize(0, 0, 500, 500) // calculate connector offsets
                        calculateNodePositions(gr.nodes)

                        // todo center graph panel on nodes by calculating their mid point
                        library = RenderGraph.library

                        addChangeListener { _, isNodePositionChange ->
                            if (!isNodePositionChange) {
                                for (n in gr.nodes) {
                                    when (n) {
                                        is ShaderGraphNode -> {
                                            n.shader?.destroy()
                                            n.shader = null
                                        }
                                        is ShaderExprNode -> {
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
                        g.draw(rv, this)
                        drawNodeConnections(x0, y0, x1, y1)
                        drawChildren(x0, y0, x1, y1)
                    }

                }, 1f)
                list
            }
        }
    }

}