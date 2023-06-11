package me.anno.graph.render.scene

import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.createMainFragmentStage
import me.anno.gpu.pipeline.LightShaders.uvwStage
import me.anno.gpu.pipeline.LightShaders.vertexI
import me.anno.gpu.pipeline.LightShaders.vertexNI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.graph.render.Texture
import me.anno.graph.render.compiler.GraphCompiler
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
import me.anno.utils.types.Booleans.toInt

class RenderLightsNode : RenderSceneNode0(
    "Render Lights",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Camera Index",
        // make them same order as outputs from RenderSceneNode
        "Vector3f", "Normal",
        "Float", "Metallic",
        "Float", "Roughness",
        "Float", "Translucency",
        "Float", "Sheen",
        "Float", "Depth",
    ),
    listOf("Texture", "Light")
) {

    val firstInputIndex = 5
    val depthIndex = firstInputIndex + 5

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, 0) // camera index
    }

    override fun invalidate() {
        framebuffer?.destroy()
        for (it in shaders) it?.first?.destroy()
        shaders.fill(null)
    }

    private val shaders = arrayOfNulls<Pair<Shader, HashMap<String, TypeValue>>>(6) // current number of shaders
    private fun getShader(type: LightType, isInstanced: Boolean): Shader {
        val id = type.ordinal.shl(1) + isInstanced.toInt()
        val shader1 = shaders[id] ?: object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {
                val sizes = intArrayOf(3, 1, 1, 1, 1, 1)
                val names = arrayOf(
                    DeferredLayerType.NORMAL,
                    DeferredLayerType.METALLIC,
                    DeferredLayerType.ROUGHNESS,
                    DeferredLayerType.TRANSLUCENCY,
                    DeferredLayerType.SHEEN,
                    DeferredLayerType.DEPTH
                )
                val expressions = sizes.indices
                    .joinToString("") { i ->
                        val sizeI = sizes[i]
                        val typeI = GLSLType.floats[sizeI - 1].glslName
                        val nameI = names[i].glslName
                        val exprI = expr(inputs!![firstInputIndex + i])
                        "$typeI $nameI=$exprI;\n"
                    }
                defineLocalVars(builder)
                val variables = typeValues.map { (k, v) -> Variable(v.type, k) } + extraVariables +
                        listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT))
                val builder = ShaderBuilder(name)
                builder.addVertex(if (isInstanced) vertexI else vertexNI)
                builder.addFragment(uvwStage)
                builder.addFragment(
                    ShaderStage(variables, expressions)
                        .add(extraFunctions.toString())
                )
                builder.addFragment(
                    ShaderStage(
                        listOf(
                            Variable(GLSLType.V2F, "uv"),
                            Variable(GLSLType.V1F, "finalDepth"),
                            Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
                        ) + depthVars,
                        "finalPosition = depthToPosition(uv,finalDepth);\n"
                    ).add(rawToDepth).add(depthToPosition)
                )
                builder.addFragment(createMainFragmentStage(type, isInstanced))
                shader = builder.create()
            }

            override val currentShader: Shader get() = shader

        }.run { shader to typeValues }

        shaders[id] = shader1
        val (shader, typeValues) = shader1
        shader.use()
        for ((k, v) in typeValues) {
            v.bind(shader, k)
        }
        return shader
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return

        val rv = renderView
        if (framebuffer?.samples != samples) {
            framebuffer?.destroy()
            framebuffer = Framebuffer(
                name, width, height, samples,
                arrayOf(TargetType.FP16Target3), DepthBufferType.NONE
            )
        }

        val framebuffer = framebuffer!!
        val renderer = Renderer.copyRenderer

        GFX.check()

        val depthTexture0 = getInput(depthIndex) as? Texture
        val depthTexture = depthTexture0?.tex as? Texture2D ?: TextureLib.depthTexture

        GFXState.useFrame(width, height, true, framebuffer, renderer) {
            val stage = pipeline.lightStage
            // todo copy depth into framebuffer
            framebuffer.clearColor(0)
            stage.bind {
                stage.draw(rv.cameraMatrix, rv.cameraPosition, rv.worldScale, ::getShader, depthTexture)
            }
        }

        setOutput(Texture(framebuffer.getTexture0()), 1)

    }
}