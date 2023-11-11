package me.anno.graph.render.scene

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.SimpleRenderer
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT

class RenderSceneNode : RenderSceneNode0(
    "Render Scene",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Stage Mask",
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting",
        "Int", "Camera Index",
        "Boolean", "Apply ToneMapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Texture", "Depth", // optional prepass
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
        setInput(4, -1) // stage mask
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
        setInput(8, 256) // bake skybox
    }

    override fun invalidate() {
        settings = null
        framebuffer?.destroy()
    }

    private var settings: DeferredSettings? = null
    lateinit var renderer: Renderer

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return

        // 0 is flow
        // val stageId = getInput(4) as Int
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int
        val applyToneMapping = getInput(7) == true

        var settings = settings
        if (settings == null || framebuffer?.samples != samples) {
            enabledLayers.clear()
            val outputs = outputs!!
            for (i in 1 until outputs.size) {
                if (isOutputUsed(outputs[i])) {
                    enabledLayers.add(DeferredLayerType.values[i - 1])
                }
                setOutput(i, null)
            }

            // this node does nothing -> just return
            if (enabledLayers.isEmpty()) return

            if (GFX.supportsDepthTextures) {
                enabledLayers.remove(DeferredLayerType.DEPTH)
            }

            // create deferred settings
            settings = DeferredSettings(enabledLayers)
            this.settings = settings
            renderer = SimpleRenderer(
                "tmp", settings,
                listOf(
                    ShaderStage(
                        "linear2srgb", listOf(
                            Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                            Variable(GLSLType.V3F, "finalEmissive", VariableMode.INOUT)
                        ), colorToSRGB
                    )
                )
            )
            framebuffer?.destroy()
            framebuffer = settings.createBaseBuffer(name, samples)
        }

        val framebuffer = framebuffer!!

        GFX.check()

        // if skybox is not used, bake it anyway?
        // -> yes, the pipeline architect (^^) has to be careful
        val skyboxResolution = getInput(8) as Int
        pipeline.bakeSkybox(skyboxResolution)

        val prepassDepth = ((getInput(9) as? Texture)?.tex as? Texture2D)?.owner

        pipeline.applyToneMapping = applyToneMapping
        val depthMode = pipeline.defaultStage.depthMode
        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            clearFramebuffer(framebuffer, prepassDepth)
            pipeline.draw()
            pipeline.defaultStage.depthMode = depthMode
            GFX.check()
        }

        // todo there are special types for which we might need to apply lighting or combine other types
        //  e.g. for forward-rendering :)

        // todo only blit textures, where necessary (e.g. only metallic might have to be blitted)

        val layers = settings.layers
        for (j in layers.indices) {
            val layer = layers[j]
            val i = DeferredLayerType.values.indexOf(layer.type) + 1
            setOutput(i, Texture.texture(framebuffer, layer.texIndex, layer.mapping, layer.type))
        }

        // get depth texture, and use it
        if (GFX.supportsDepthTextures) {
            val i = DeferredLayerType.values.indexOf(DeferredLayerType.DEPTH) + 1
            setOutput(i, Texture.depth(framebuffer, "r", DeferredLayerType.DEPTH))
        }
    }

    fun clearFramebuffer(framebuffer: IFramebuffer, prepassDepth: Framebuffer?) {
        val color = 0
        if (prepassDepth != null && // else wouldn't work well
            prepassDepth.width == framebuffer.width &&
            prepassDepth.height == framebuffer.height &&
            prepassDepth.samples == framebuffer.samples
        ) {
            framebuffer.clearColor(color)
            prepassDepth.copyTo(framebuffer, GL_DEPTH_BUFFER_BIT)
            pipeline.defaultStage.depthMode = DepthMode.EQUALS
        } else {
            framebuffer.clearColor(color, depth = true)
        }
    }
}