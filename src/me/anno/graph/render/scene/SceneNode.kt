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
import me.anno.gpu.pipeline.Sorting
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
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting",
        "Int", "Camera Index",
        "Boolean", "Apply ToneMapping",
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
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
    }

    fun invalidate() {
        settings = null
        framebuffer?.destroy()
        framebuffer = null
    }

    private var settings: DeferredSettingsV2? = null

    override fun executeAction() {
        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return
        // 0 is flow
        val stageId = getInput(4) as Int
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int

        var settings = settings
        if (settings == null || settings.samples != samples) {
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
            settings = DeferredSettingsV2(enabledLayers, samples, true)
        }

        val rv: RenderView = renderView

        var framebuffer = framebuffer
        if (framebuffer == null || framebuffer.w != width || framebuffer.h != height) {
            framebuffer?.destroy()
            framebuffer = settings.createBaseBuffer()
            this.framebuffer = framebuffer
        }

        // todo keep framebuffer, if it stayed the same as last frame

        val renderer = Renderer("", settings)

        GFX.check()

        val hdr = true // todo hdr?
        pipeline.applyToneMapping = !hdr

        GFXState.useFrame(width, height, true, framebuffer, renderer) {

            rv.clearColorOrSky(rv.cameraMatrix)
            GFX.check()
            pipeline.stages[stageId].bindDraw(pipeline)
            GFX.check()

        }

        // todo there are special types for which we might need to apply lighting or combine other types
        for (layer in settings.layers) {
            val tex = framebuffer.getTextureI(layer.index)
            if (tex is Texture2D && !tex.isCreated) {
                LOGGER.warn("${layer.type} -> ${layer.index} is missing")
                continue
            }
            val i = DeferredLayerType.values.indexOf(layer.type) + 1
            setOutput(Texture(tex, layer.mapping), i)
        }

    }
}