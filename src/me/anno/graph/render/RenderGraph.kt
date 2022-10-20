package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.ui.GraphPanel
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

    class SceneNode : ActionNode(
        "Scene",
        listOf(
            "Int", "Stage Id",
            "Int", "Sorting", // todo enum (?)
            "Int", "Camera Index",
        ) + DeferredLayerType.values.map { listOf("Texture", it.name) }.flatten(),
        // list all available deferred layers
        DeferredLayerType.values.map { listOf("Texture", it.name) }.flatten()
    ) {

        lateinit var renderView: RenderView
        lateinit var pipeline: Pipeline

        val enabledLayers = ArrayList<DeferredLayerType>()
        var framebuffer: IFramebuffer? = null
        override fun executeAction(graph: FlowGraph) {
            // 0 is flow
            val stageId = getInput(graph, 1) as Int
            val sorting = getInput(graph, 2) as Int
            val cameraIndex = getInput(graph, 3) as Int
            enabledLayers.clear()
            val outputs = outputs!!
            for (i in 1 until outputs.size) {
                val output = outputs[i]
                if (output.others.isNotEmpty()) {
                    // todo only enable it, if the value actually will be used
                    enabledLayers.add(DeferredLayerType.values[i - 1])
                }
                setOutput(null, i)
            }

            if (enabledLayers.isEmpty()) {
                return
            }

            // create deferred settings
            // todo keep settings if they stayed the same as last frame
            val settings = DeferredSettingsV2(enabledLayers, true)

            val rv: RenderView = renderView
            val camera: Camera = rv.editorCamera

            val dstBuffer = settings.createBaseBuffer()
            framebuffer?.destroy()
            framebuffer = dstBuffer

            val renderer = Renderer("", settings)

            GFX.check()

            val hdr = true // todo hdr?
            pipeline.applyToneMapping = !hdr

            val depthMode = DepthMode.ALWAYS
            val w = 500
            val h = 500
            val changeSize = true
            GFXState.useFrame(w, h, changeSize, dstBuffer, renderer) {

                Frame.bind()
                GFXState.depthMode.use(depthMode) {
                    rv.setClearDepth()
                    dstBuffer.clearDepth()
                }

                rv.clearColor(camera, camera, 0f, hdr)
                GFX.check()
                pipeline.stages[stageId]
                    .bindDraw(pipeline)
                GFX.check()

            }

            for (i in 1 until outputs.size) {
                val output = outputs[i]
                if (output.others.isNotEmpty()) {
                    // define output value
                    // todo there are special types for which we might need to apply lighting or combine other types
                    val type = DeferredLayerType.values[i - 1]
                    val layer = settings.findLayer(type)!!
                    val tex = dstBuffer.getTextureI(layer.index)
                    if (tex is Texture2D && !tex.isCreated) {
                        LOGGER.warn("$type -> ${layer.index} is missing")
                        continue
                    }
                    val output1: Any = if (layer.mapping.isEmpty()) tex
                    else {
                        val output1 = Texture("map", listOf(layer.mapping))
                        output1.v2d = tex
                        output1
                    }
                    setOutput(output1, i)
                }
            }

        }
    }

    // todo node to apply lights on deferred rendering
    // is the same as SDR / HDR

    val graph = FlowGraph()

    var scene: Entity? = null

    fun createUI(style: Style): Panel {
        val panel = GraphPanel(graph, style)
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

        val start = graph.inputs.firstOrNull2() as? StartNode
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
            ) + NodeLibrary.flowNodes.nodes,
        )

        @JvmStatic
        fun main(args: Array<String>) {

            val g = RenderGraph()
            val gr = g.graph

            // todo add start to library, or make non-deletable
            // todo add return to library

            gr.inputs.add(StartNode())
            gr.nodes.add(SceneNode())
            gr.outputs.add(ReturnNode(listOf("Texture", "Result")))
            gr.nodes.addAll(gr.inputs)
            gr.nodes.addAll(gr.outputs)
            gr.inputs.first().connectTo(gr.nodes.first())
            gr.nodes.first().connectTo(0, gr.outputs.first(), 0)
            gr.nodes.first().connectTo(2, gr.outputs.first(), 1)

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
                list.add(object : GraphPanel(gr, style) {

                    init {

                        // calculate initial node positions
                        calculateSize(500, 500)
                        setPosSize(0, 0, 500, 500) // calculate connector offsets
                        calculateNodePositions(gr.nodes)

                        // todo center graph panel on nodes by calculating their mid point

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