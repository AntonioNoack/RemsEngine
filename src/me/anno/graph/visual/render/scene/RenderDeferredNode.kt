package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.SimpleRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.compiler.GraphShader
import me.anno.graph.visual.render.scene.utils.CopyInputsOrClear.Companion.bindCopyShader
import me.anno.graph.visual.render.scene.utils.CopyInputsOrClear.Companion.hasNonDepthInputs
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager

open class RenderDeferredNode : RenderViewNode(
    "RenderDeferred",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Boolean", "Apply Tone Mapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Enum<me.anno.graph.visual.render.scene.DrawSkyMode>", "Draw Sky"
    ) + inList,
    // list all available deferred layers
    outList
) {

    companion object {
        private val LOGGER = LogManager.getLogger(RenderDecalsNode::class)
        fun listLayers(type: String?): List<String> {
            return DeferredLayerType.values.flatMap {
                listOf(
                    type ?: when (it.workDims) {
                        1 -> "Float"
                        2 -> "Vector2f"
                        3 -> "Vector3f"
                        else -> "Vector4f"
                    }, if (it.name == "Color") "Diffuse" else it.name
                )
            }
        }

        val inList = listLayers(null)
        val outList = listLayers("Texture")

        val firstInputIndex = 8
        val firstOutputIndex = 1

        val depthInputIndex = firstInputIndex + DeferredLayerType.values.indexOf(DeferredLayerType.DEPTH)
    }

    val enabledLayers = ArrayList<DeferredLayerType>()

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage
        setInput(5, false) // don't apply tonemapping
        setInput(6, 0) // skybox resolution
        setInput(7, DrawSkyMode.DONT_DRAW_SKY)
    }

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val stage get() = getInput(4) as PipelineStage
    val applyToneMapping get() = getBoolInput(5)
    val skyResolution get() = getIntInput(6)
    val drawSky get() = getInput(7) as DrawSkyMode

    override fun invalidate() {
        settings = null
        for ((_, v) in shaders) v.first.destroy()
        shaders.clear()
    }

    private var settings: DeferredSettings? = null
    lateinit var renderer: Renderer

    /**
     * returns null, if there aren't any outputs
     * */
    fun defineFramebuffer(): IFramebuffer? {
        var settings = settings
        val samples = samples
        if (settings == null || framebuffer?.samples != samples) {
            enabledLayers.clear()
            val outputs = outputs
            for (i in 1 until outputs.size) {
                if (isOutputUsed(outputs[i])) {
                    enabledLayers.add(DeferredLayerType.values[i - 1])
                }
                setOutput(i, null)
            }

            // this node does nothing -> just return
            if (enabledLayers.isEmpty()) {
                LOGGER.warn("$name does nothing")
                return null
            }

            if (GFX.supportsDepthTextures) {
                enabledLayers.remove(DeferredLayerType.DEPTH)
            }

            // create deferred settings
            settings = DeferredSettings(enabledLayers)
            this.settings = settings
            renderer = SimpleRenderer(
                "rsdn", settings,
                listOf(
                    ShaderStage(
                        "linear2srgb", listOf(
                            Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                            Variable(GLSLType.V3F, "finalEmissive", VariableMode.INOUT)
                        ), colorToSRGB
                    )
                )
            )
        }
        createNewFramebuffer(settings, samples)
        return framebuffer
    }

    open fun createNewFramebuffer(settings: DeferredSettings, samples: Int) {
        framebuffer = settings.getBaseBufferFBStack(name, width, height, samples)
    }

    private fun needsToRenderSky(): Boolean {
        return skyResolution > 0 && drawSky != DrawSkyMode.DONT_DRAW_SKY
    }

    override fun executeAction() {
        if (width < 1 || height < 1) return
        if (needsRendering(stage) || needsToRenderSky()) executeRendering()
        else skipRendering()
    }

    fun skipRendering() {
        // just copy inputs to outputs
        for (i in 0 until outList.size.shr(1)) {
            setOutput(firstOutputIndex + i, getInput(firstInputIndex + i))
        }
    }

    fun executeRendering() {
        val framebuffer = defineFramebuffer() ?: return
        timeRendering(name, timer) {
            pipeline.bakeSkybox(skyResolution)
            copyInputsOrClear(framebuffer)
            GFXState.useFrame(width, height, true, framebuffer, renderer) {
                render()
            }
            setOutputs(framebuffer)
        }
    }

    fun render() {
        val drawSky = drawSky
        if (drawSky == DrawSkyMode.BEFORE_GEOMETRY) pipeline.drawSky()
        pipeline.applyToneMapping = applyToneMapping
        if (needsRendering(stage)) {
            val stage = pipeline.stages.getOrNull(stage.id)
            if (stage != null) {
                val oldDepth = stage.depthMode
                if (hasDepthPrepass()) stage.depthMode = oldDepth.equalsMode
                stage.bindDraw(pipeline)
                stage.depthMode = oldDepth
            }
        }
        if (drawSky == DrawSkyMode.AFTER_GEOMETRY) pipeline.drawSky()
    }

    fun setOutputs(framebuffer: IFramebuffer) {
        if (framebuffer.depthBufferType != DepthBufferType.NONE) {
            pipeline.prevDepthBuffer = framebuffer
        }
        val layers = settings!!.semanticLayers
        for (j in layers.indices) {
            val layer = layers[j]
            val i = DeferredLayerType.values.indexOf(layer.type) + 1
            setOutput(i, Texture.texture(framebuffer, layer.texIndex, layer.mapping, layer.type))
        }
        // get depth texture, and use it
        if (GFX.supportsDepthTextures) {
            val i = DeferredLayerType.values.indexOf(DeferredLayerType.DEPTH) + 1
            setOutput(i, Texture.depth(framebuffer))
        }
    }

    private val shaders = HashMap<Renderer, GraphShader>()

    fun hasDepthPrepass(): Boolean {
        val prepassDepthT = getInput(depthInputIndex) as? Texture
        val prepassDepth = prepassDepthT.texOrNull
        return prepassDepth != null && inputs[0].others.any2 { it.node is DepthPrepassNode }
    }

    open fun copyInputsOrClear(framebuffer: IFramebuffer) {
        copyDepthInput(framebuffer)
        // if all inputs are null, we can skip this
        if (hasNonDepthInputs(inputs)) {
            copyNonDepthInputs(framebuffer)
        }
    }

    fun copyDepthInput(framebuffer: IFramebuffer) {
        val prepassDepthT = getInput(depthInputIndex) as? Texture
        val prepassDepth = prepassDepthT.texOrNull
        if (prepassDepth != null) {
            GFXState.useFrame(framebuffer, Renderer.copyRenderer) {
                Blitting.copyColorAndDepth(blackTexture, prepassDepth, prepassDepthT.mask1Index, false)
            }
            // todo we need a flag whether this is a prepass
            // pipeline.defaultStage.depthMode = DepthMode.EQUALS
        } else {
            // set depth mode to clear the correct depth value (could be 0 or 1)
            GFXState.depthMode.use(renderView.depthMode) {
                framebuffer.clearColor(0, depth = true)
            }
        }
    }

    fun copyNonDepthInputs(framebuffer: IFramebuffer) {
        GFXState.useFrame(framebuffer, renderer) {
            GFXState.depthMode.use(alwaysDepthMode) {
                GFXState.depthMask.use(false) {
                    val shader = bindCopyShader(inputs, outputs, name, graph as FlowGraph, shaders)
                    bindDepthUniforms(shader)
                    flat01.draw(shader)
                }
            }
        }
    }
}