package me.anno.graph.visual.render.scene

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader.Companion.getKey
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.SimpleRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager

open class RenderDeferredNode : RenderViewNode(
    "RenderDeferred",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting", // ignored for now
        "Int", "Camera Index", // ignored for now
        "Boolean", "Apply ToneMapping",
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
                        4 -> "Vector4f"
                        else -> throw IllegalStateException()
                    }, if (it.name == "Color") "Diffuse" else it.name
                )
            }
        }

        val inList = listLayers(null)
        val outList = listLayers("Texture")

        val firstInputIndex = 10
        val firstOutputIndex = 1
    }

    val enabledLayers = ArrayList<DeferredLayerType>()

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
        setInput(8, 0)
        setInput(9, DrawSkyMode.DONT_DRAW_SKY)
    }

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val skyResolution get() = getIntInput(8)

    val stage get() = getInput(4) as PipelineStage
    val drawSky get() = getInput(9) as DrawSkyMode

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
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
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

    override fun executeAction() {

        if (width < 1 || height < 1) return

        if (!needsRendering() && skyResolution <= 0 && drawSky == DrawSkyMode.DONT_DRAW_SKY) {
            // just copy inputs to outputs
            for (i in 0 until outList.size.shr(1)) {
                setOutput(firstOutputIndex + i, getInput(firstInputIndex + i))
            }
            return
        }

        val framebuffer = defineFramebuffer() ?: return
        timeRendering("$name-$stage", timer) {
            bakeSkybox()
            copyInputsOrClear(framebuffer)
            bind(framebuffer) {
                render()
            }
            setOutputs(framebuffer)
        }
    }

    fun bind(framebuffer: IFramebuffer, run: () -> Unit) {
        GFXState.useFrame(width, height, true, framebuffer, renderer, run)
    }

    fun bakeSkybox() {
        pipeline.bakeSkybox(skyResolution)
    }

    fun needsRendering(): Boolean {
        val stage = getInput(4) as PipelineStage
        val stage1 = pipeline.stages.getOrNull(stage.id) ?: return false
        return !stage1.isEmpty()
    }

    fun render() {
        val drawSky = drawSky
        if (drawSky == DrawSkyMode.BEFORE_GEOMETRY) {
            pipeline.drawSky()
        }
        pipeline.applyToneMapping = getBoolInput(7)
        if (needsRendering()) {
            pipeline.stages.getOrNull(stage.id)?.bindDraw(pipeline)
        }
        if (drawSky == DrawSkyMode.AFTER_GEOMETRY) {
            pipeline.drawSky()
        }
    }

    fun setOutputs(framebuffer: IFramebuffer) {
        val layers = settings!!.semanticLayers
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

    private val shaders = HashMap<Renderer, Pair<Shader, Map<String, TypeValue>>>()

    fun getOutputs(): List<IndexedValue<DeferredLayerType>> {
        return DeferredLayerType.values.withIndex()
            .filter { (index, _) ->
                !inputs[index + firstInputIndex].isEmpty() &&
                        isOutputUsed(outputs[index + firstOutputIndex])
            }
    }

    private fun bindShader(): Shader {
        val renderer = GFXState.currentRenderer
        val shader1 = shaders.getOrPut(renderer) {
            object : GraphCompiler(graph as FlowGraph) {

                // not being used, as we only have an expression
                override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

                val shader: Shader

                init {

                    // to do: filter for non-composite types
                    // only load what is given? -> yes :D

                    val output0 = getOutputs()
                    val outputs = renderer.deferredSettings!!.semanticLayers.toList()
                        .map { tt -> output0.first { it.value == tt.type } }

                    if (DeferredLayerType.CLICK_ID in outputs.map { it.value } ||
                        DeferredLayerType.GROUP_ID in outputs.map { it.value }) {
                        extraVariables.add(Variable(GLSLType.V4F, "finalId", VariableMode.INOUT))
                    }

                    assertTrue(builder.isEmpty())
                    var expressions = outputs.joinToString("") { (i, type) ->
                        val nameI = type.glslName
                        expr(inputs[firstInputIndex + i])
                        val exprI = builder.toString()
                        builder.clear()
                        "$nameI = $exprI;\n"
                    }

                    if (outputs.any2 { it.value == DeferredLayerType.DEPTH }) {
                        expressions += "gl_FragDepth = depthToRaw(finalDepth);\n"
                    }

                    defineLocalVars(builder)

                    extraVariables.add(Variable(GLSLType.V2F, "uv"))

                    val variables = outputs
                        .map { (_, type) ->
                            val typeI = GLSLType.floats[type.workDims - 1]
                            val nameI = type.glslName
                            Variable(typeI, nameI, VariableMode.OUT)
                        } + typeValues.map { (k, v) -> Variable(v.type, k) } + extraVariables

                    val builder = ShaderBuilder(name)
                    builder.addVertex(
                        ShaderStage(
                            "simple-triangle", listOf(
                                Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                                Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                            ), "gl_Position = vec4(coords*2.0-1.0,0.0,1.0);\n" +
                                    "uv = coords;\n"
                        )
                    )

                    builder.settings = renderer.deferredSettings
                    builder.useRandomness = false
                    builder.addFragment(
                        ShaderStage("rsdn-expr", variables, expressions)
                            .add(extraFunctions.toString())
                    )
                    builder.ignored.add("d_camRot")
                    builder.ignored.add("reverseDepth")

                    shader = builder.create(getKey(), "rsdn-${outputs.joinToString { it.value.name }}")
                }

                override val currentShader: Shader get() = shader
            }.finish()
        }

        val (shader, typeValues) = shader1
        shader.use()
        for ((k, v) in typeValues) {
            v.bind(shader, k)
        }
        return shader
    }

    private fun hasAnyInput(): Boolean {
        for (i in 0 until inList.size.shr(1)) {
            if (!inputs[firstInputIndex + i].isEmpty()) {
                return true
            }
        }
        return false
    }

    open fun copyInputsOrClear(framebuffer: IFramebuffer) {
        val inputIndex = firstInputIndex + inList.indexOf(DeferredLayerType.DEPTH.name).shr(1)
        val prepassDepth = (getInput(inputIndex) as? Texture)?.texOrNull
        if (prepassDepth != null) {
            GFXState.useFrame(framebuffer, Renderer.copyRenderer) {
                GFX.copyColorAndDepth(blackTexture, prepassDepth)
            }
            // todo we need a flag whether this is a prepass
            // pipeline.defaultStage.depthMode = DepthMode.EQUALS
        } else {
            framebuffer.clearColor(0, depth = true)
        }
        // if all inputs are null, we can skip this
        if (hasAnyInput()) {
            GFXState.useFrame(framebuffer, renderer) {
                GFXState.depthMode.use(alwaysDepthMode) {
                    GFXState.depthMask.use(false) {
                        val shader = bindShader()
                        bindDepthUniforms(shader)
                        flat01.draw(shader)
                    }
                }
            }
        }
    }
}