package me.anno.graph.render.scene

import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.LightShaders.combineFStage
import me.anno.gpu.pipeline.LightShaders.combineLighting1
import me.anno.gpu.pipeline.LightShaders.combineVStage
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib.blackCube
import me.anno.graph.render.Texture
import me.anno.graph.render.compiler.GraphCompiler
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode

class CombineLightsNode : RenderSceneNode0(
    "Combine Lights",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Bool", "Apply Tone Mapping",
        // make them same order as outputs from RenderSceneNode & RenderLightsNode
        "Vector3f", "Light",
        "Vector3f", "Diffuse",
        "Vector3f", "Emissive",
        "Float", "Occlusion",
        "Float", "Ambient Occlusion",
    ), listOf("Texture", "Illuminated")
) {

    val firstInputIndex = 5

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, false) // apply tone mapping
    }

    override fun invalidate() {
        shader?.first?.destroy()
        shader = null
    }

    private var shader: Pair<Shader, HashMap<String, TypeValue>>? = null // current number of shaders
    private fun bindShader(skybox: CubemapTexture): Shader {
        val shader1 = shader ?: object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {
                val sizes = intArrayOf(3, 3, 3, 1, 1)
                val names = arrayOf(
                    DeferredLayerType.LIGHT_SUM.glslName,
                    DeferredLayerType.COLOR.glslName,
                    DeferredLayerType.EMISSIVE.glslName,
                    DeferredLayerType.OCCLUSION.glslName,
                    "ambientOcclusion"
                )
                val expressions = sizes.indices
                    .joinToString("") { i ->
                        val nameI = names[i]
                        val exprI = expr(inputs!![firstInputIndex + i])
                        "$nameI=$exprI;\n"
                    }
                defineLocalVars(builder)
                val variables = typeValues.map { (k, v) -> Variable(v.type, k) } + listOf(
                    Variable(GLSLType.V2F, "uv"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT)
                ) + depthVars + sizes.indices.map { i ->
                    val sizeI = sizes[i]
                    val typeI = GLSLType.floats[sizeI - 1]
                    val nameI = names[i]
                    Variable(typeI, nameI, VariableMode.OUT)
                }
                val builder = ShaderBuilder(name)
                builder.addVertex(combineVStage)
                builder.addFragment(
                    ShaderStage("combineLightsExpr", variables, expressions)
                        .add(extraFunctions.toString())
                )
                builder.addFragment(combineFStage)
                builder.ignored.addAll(listOf("tint", "d_camRot"))
                shader = builder.create("cmb1")
            }

            override val currentShader: Shader get() = shader
        }.run { shader to typeValues }

        shader = shader1
        val (shader2, typeValues) = shader1
        shader2.use()
        skybox.bind(shader2, "skybox", GPUFiltering.LINEAR, Clamping.CLAMP)
        for ((k, v) in typeValues) {
            v.bind(shader2, k)
        }
        return shader2
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return

        val applyToneMapping = getInput(4) == true

        val framebuffer = FBStack[name, width, height, 3, BufferQuality.HIGH_16, samples, DepthBufferType.NONE]
        val renderer = Renderer.copyRenderer

        GFX.check()

        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            renderPurely2 {
                val shader = bindShader(pipeline.bakedSkybox?.getTexture0() ?: blackCube)
                combineLighting1(shader, applyToneMapping)
            }
        }

        setOutput(1, Texture(framebuffer.getTexture0()))
    }
}