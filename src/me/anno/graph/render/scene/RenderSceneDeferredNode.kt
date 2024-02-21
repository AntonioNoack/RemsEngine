package me.anno.graph.render.scene

import me.anno.ecs.components.mesh.TypeValue
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
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
import me.anno.graph.render.Texture
import me.anno.graph.render.compiler.GraphCompiler
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
import me.anno.utils.structures.lists.Lists.any2

class RenderSceneDeferredNode : RenderViewNode(
    "Render Scene",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Enum<me.anno.gpu.pipeline.Sorting>", "Sorting",
        "Int", "Camera Index",
        "Boolean", "Apply ToneMapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Int", "Draw Sky", // -1 = before, 0 = don't, 1 = after
    ) + inList,
    // list all available deferred layers
    outList
) {

    companion object {
        fun listLayers(type: String?): List<String> {
            return DeferredLayerType.values.map {
                listOf(
                    type ?: when (it.workDims) {
                        1 -> "Float"
                        2 -> "Vector2f"
                        3 -> "Vector3f"
                        4 -> "Vector4f"
                        else -> throw IllegalStateException()
                    }, if (it.name == "Color") "Diffuse" else it.name
                )
            }.flatten()
        }

        val inList = listLayers(null)
        val outList = listLayers("Texture")
    }

    val enabledLayers = ArrayList<DeferredLayerType>()

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE) // stage mask
        setInput(5, Sorting.NO_SORTING)
        setInput(6, 0) // camera index
        setInput(7, false) // apply tonemapping
        setInput(8, 0) // don't bake skybox
    }

    override fun invalidate() {
        settings = null
        framebuffer?.destroy()
        for ((_, v) in shaders) v.first.destroy()
        shaders.clear()
    }

    private var settings: DeferredSettings? = null
    lateinit var renderer: Renderer

    fun defineFramebuffer() {
        var settings = settings
        val samples = getInput(3) as Int
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
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return

        val stage = getInput(4) as PipelineStage
        // val sorting = getInput(5) as Int
        // val cameraIndex = getInput(6) as Int
        val applyToneMapping = getInput(7) == true

        defineFramebuffer()

        val framebuffer = framebuffer!!

        GFX.check()

        // if skybox is not used, bake it anyway?
        // -> yes, the pipeline architect (^^) has to be careful
        val skyboxResolution = getInput(8) as Int
        pipeline.bakeSkybox(skyboxResolution)

        val drawSky = getInput(9) as Int

        pipeline.applyToneMapping = applyToneMapping
        val depthMode = pipeline.defaultStage.depthMode
        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            defineInputs(framebuffer)
            if (drawSky == -1) {
                pipeline.drawSky()
            }
            pipeline.stages.getOrNull(stage.id)?.bindDraw(pipeline)
            if (drawSky == 1) {
                pipeline.drawSky()
            }
            pipeline.defaultStage.depthMode = depthMode
            GFX.check()
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
            setOutput(i, Texture.depth(framebuffer, "r", DeferredLayerType.DEPTH))
        }
    }

    private val shaders = HashMap<Renderer, Pair<Shader, Map<String, TypeValue>>>()

    val firstInputIndex = 10
    val firstOutputIndex = 1

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

                    var expressions = outputs.joinToString("") { (i, type) ->
                        val nameI = type.glslName
                        val exprI = expr(inputs[firstInputIndex + i])
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

                    shader = builder.create("rsdn-${outputs.joinToString { it.value.name }}")
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

    fun defineInputs(framebuffer: IFramebuffer) {
        val inputIndex = firstInputIndex + inList.indexOf(DeferredLayerType.DEPTH.name).shr(1)
        val prepassDepth = (getInput(inputIndex) as? Texture)?.tex
        if (prepassDepth != null) {
            GFX.copyColorAndDepth(blackTexture, prepassDepth)
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