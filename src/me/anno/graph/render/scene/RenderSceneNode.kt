package me.anno.graph.render.scene

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.utils.LOGGER

class RenderSceneNode : RenderSceneNode0(
    "Render Scene",
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
    DeferredLayerType.values.map {
        listOf("Texture", if (it.name == "Color") "Diffuse" else it.name)
    }.flatten()
) {

    val enabledLayers = ArrayList<DeferredLayerType>()

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, 0) // stage id
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
    }

    override fun invalidate() {
        settings = null
        framebuffer?.destroy()
    }

    private var settings: DeferredSettingsV2? = null
    lateinit var renderer: Renderer

    override fun executeAction() {
        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return
        // 0 is flow
        val stageId = getInput(4) as Int
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int
        val applyToneMapping = getInput(7) == true

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

            if (enabledLayers.isEmpty()) return

            enabledLayers.remove(DeferredLayerType.DEPTH)

            // create deferred settings
            settings = DeferredSettingsV2(enabledLayers, samples, true)
            this.settings = settings
            renderer = Renderer("tmp", settings)
            framebuffer?.destroy()
            framebuffer = settings.createBaseBuffer(name)
        }

        val renderView = renderView
        val framebuffer = framebuffer!!

        GFX.check()

        pipeline.applyToneMapping = applyToneMapping
        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            renderView.clearColorOrSky(renderView.cameraMatrix)
            GFX.check()
            pipeline.stages[stageId].bindDraw(pipeline)
            GFX.check()
        }

        // todo there are special types for which we might need to apply lighting or combine other types
        //  e.g. for forward-rendering :)
        val layers = settings.layers
        for (j in layers.indices) {
            val layer = layers[j]
            val tex = framebuffer.getTextureI(layer.texIndex)
            if (tex is Texture2D && !tex.isCreated) {
                LOGGER.warn("${layer.type} -> ${layer.texIndex} is missing")
                continue
            }
            val i = DeferredLayerType.values.indexOf(layer.type) + 1
            setOutput(Texture(tex, layer.mapping, layer.type), i)
        }

        // get depth texture, and use it
        val buf0 = (framebuffer as? Framebuffer)?.ssBuffer
        val buf1 = (framebuffer as? MultiFramebuffer)?.targetsI?.first()?.ssBuffer
        val buf2 = buf0 ?: buf1 ?: framebuffer
        val tex = buf2.depthTexture!!
        val i = DeferredLayerType.values.indexOf(DeferredLayerType.DEPTH) + 1
        setOutput(Texture(tex, "r", DeferredLayerType.DEPTH), i)

    }
}