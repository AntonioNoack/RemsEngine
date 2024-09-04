package me.anno.graph.visual.render.scene

import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.createMainFragmentStage
import me.anno.gpu.pipeline.LightShaders.invStage
import me.anno.gpu.pipeline.LightShaders.uvwStage
import me.anno.gpu.pipeline.LightShaders.vertexI
import me.anno.gpu.pipeline.LightShaders.vertexNI
import me.anno.gpu.shader.BaseShader.Companion.getKey
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt

/**
 * collects the lights within a scene
 * */
class RenderLightsNode : RenderViewNode(
    "Render Lights",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Camera Index",
        // make them same order as outputs from RenderSceneNode
        "Vector3f", "Normal",
        "Float", "Reflectivity",
        "Float", "Translucency",
        "Float", "Sheen",
        "Float", "Depth",
    ),
    listOf("Texture", "Light")
) {

    val firstInputIndex = 5
    val depthIndex = firstInputIndex + 4

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, 0) // camera index
    }

    override fun invalidate() {
        for (it in shaders) it?.first?.destroy()
        shaders.fill(null)
    }

    private val shaders =
        arrayOfNulls<Pair<Shader, Map<String, TypeValue>>>(LightType.entries.size.shl(1)) // current number of shaders

    private fun getShader(type: LightType, isInstanced: Boolean): Shader {
        val id = type.ordinal.shl(1) + isInstanced.toInt()
        val shader1 = shaders[id] ?: object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {

                val types = listOf(
                    DeferredLayerType.NORMAL,
                    DeferredLayerType.REFLECTIVITY,
                    DeferredLayerType.TRANSLUCENCY,
                    DeferredLayerType.SHEEN,
                    DeferredLayerType.DEPTH
                )

                assertTrue(builder.isEmpty())
                val expressions = types.indices
                    .joinToString("") { i ->
                        val nameI = types[i].glslName
                        expr(inputs[firstInputIndex + i])
                        val exprI = builder.toString()
                        builder.clear()
                        "$nameI = $exprI;\n"
                    } + "if(finalDepth > 1e38) discard;\n" // sky doesn't need lighting

                defineLocalVars(builder)

                val variables = types.indices.map { i ->
                    val typeI = GLSLType.floats[types[i].workDims - 1]
                    val nameI = types[i].glslName
                    Variable(typeI, nameI, VariableMode.OUT)
                } + typeValues.map { (k, v) -> Variable(v.type, k) } +
                        extraVariables +
                        listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT))

                val builder = ShaderBuilder(name)
                builder.addVertex(if (isInstanced) vertexI else vertexNI)
                if (isInstanced) builder.addFragment(invStage)

                builder.addFragment(uvwStage)
                builder.addFragment(
                    ShaderStage("r-light-f0", variables, expressions)
                        .add(extraFunctions.toString())
                )

                builder.addFragment(
                    ShaderStage(
                        "r-light-f1", listOf(
                            Variable(GLSLType.V2F, "uv"),
                            Variable(GLSLType.V1F, "finalDepth"),
                            Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
                        ) + depthVars,
                        "finalPosition = depthToPosition(uv,finalDepth);\n"
                    ).add(rawToDepth).add(depthToPosition)
                )

                builder.addFragment(createMainFragmentStage(type, isInstanced))
                shader = builder.create(getKey(), "${type.ordinal}-${isInstanced.toInt()}")
            }

            override val currentShader: Shader get() = shader
        }.finish()

        shaders[id] = shader1
        val (shader, typeValues) = shader1
        shader.use()
        for ((k, v) in typeValues) {
            v.bind(shader, k)
        }
        return shader
    }

    override fun executeAction() {

        // default output in case of error
        setOutput(1, null)

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        val depthTexture = getInput(depthIndex) as? Texture
        val depthT = depthTexture.texOrNull ?: return
        val depthM = depthTexture.mask1Index

        val useDepth = true
        val framebuffer = FBStack[
            name, width, height, TargetType.Float16x3, samples,
            if (useDepth) DepthBufferType.INTERNAL else DepthBufferType.NONE
        ]

        timeRendering(name, timer) {
            useFrame(width, height, true, framebuffer, copyRenderer) {
                val stage = pipeline.lightStage
                GFX.copyColorAndDepth(blackTexture, depthT, depthM)
                stage.bind {
                    stage.draw(
                        pipeline, RenderState.cameraMatrix, RenderState.cameraPosition, RenderState.worldScale,
                        ::getShader, depthT, depthTexture.mask
                    )
                }
            }
            setOutput(1, Texture.texture(framebuffer, 0, "rgb", DeferredLayerType.LIGHT_SUM))
        }
    }
}