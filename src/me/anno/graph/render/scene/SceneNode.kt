package me.anno.graph.render.scene

import me.anno.ecs.components.camera.Camera
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.LOGGER

class SceneNode : ActionNode(
    "Scene",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Stage Id",
        "Int", "Sorting", // todo enum
        "Int", "Camera Index",
        "Bool", "Apply ToneMapping",
    ),
    // list all available deferred layers
    DeferredLayerType.values.map { listOf("Texture", it.name) }.flatten()
) {

    lateinit var renderView: RenderView
    lateinit var pipeline: Pipeline

    val enabledLayers = ArrayList<DeferredLayerType>()
    var framebuffer: IFramebuffer? = null

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, 0) // stage id
    }

    override fun executeAction() {
        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return
        // 0 is flow
        val stageId = getInput(4) as Int
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int
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
        val settings = DeferredSettingsV2(enabledLayers, samples, true)

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
        GFXState.useFrame(width, height, true, dstBuffer, renderer) {

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