package me.anno.graph.visual.render.scene

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.LightShaders.combineLighting
import me.anno.gpu.pipeline.LightShaders.combineVStage
import me.anno.gpu.shader.BaseShader.Companion.getKey
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.roughnessIfMissing
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.blackCube
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue

/**
 * combines diffuse, emissive, and light into a cell-shaded final color
 * */
class CellShadingNode : RenderViewNode(
    "Cell Shading",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Bool", "Apply Tone Mapping",
        // make them same order as outputs from RenderSceneNode & RenderLightsNode
        "Vector3f", "Light",
        "Vector3f", "Diffuse",
        "Vector3f", "Emissive",
        "Float", "Reflectivity",
        "Float", "Ambient Occlusion",
    ), listOf("Texture", "Illuminated")
) {

    companion object {
        val combineFStageCellShaded = ShaderStage(
            "combineLight-cellShading-f", listOf(
                Variable(GLSLType.V3F, "finalColor", VariableMode.INMOD),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.INMOD),
                Variable(GLSLType.SCube, "reflectionMap"),
                Variable(GLSLType.V3F, "finalNormal"),
                Variable(GLSLType.V1F, "finalReflectivity"),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.V3F, "finalLight"),
                Variable(GLSLType.V1F, "ambientOcclusion"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "" +
                    colorToLinear +
                    roughnessIfMissing +
                    "   vec3 light = finalLight + sampleSkyboxForAmbient(finalNormal, finalRoughness, finalReflectivity);\n" +
                    "   float light0 = max(brightness(light), 1e-38);\n" +
                    "   float power = 3.333;\n" +
                    "   float light0Scale = pow(2.0*power, clamp(round(log2(light0)/power), -3.0, 4.0));\n" +
                    "   finalColor = finalColor * light0Scale + finalEmissive;\n" +
                    "   if(applyToneMapping) finalColor = tonemapLinear(finalColor);\n" +
                    colorToSRGB +
                    "   color = vec4(finalColor, 1.0);\n"
        ).add(tonemapGLSL).add(getReflectivity).add(sampleSkyboxForAmbient).add(brightness)
    }

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

    private var shader: Pair<Shader, Map<String, TypeValue>>? = null // current number of shaders
    private fun bindShader(skybox: CubemapTexture): Shader {
        val shader1 = shader ?: object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {
                val sizes = intArrayOf(3, 3, 3, 1, 1)
                val names = listOf(
                    DeferredLayerType.LIGHT_SUM.glslName,
                    DeferredLayerType.COLOR.glslName,
                    DeferredLayerType.EMISSIVE.glslName,
                    DeferredLayerType.REFLECTIVITY.glslName,
                    "ambientOcclusion"
                )
                assertTrue(builder.isEmpty())
                val expressions = sizes.indices
                    .joinToString("") { i ->
                        val nameI = names[i]
                        expr(inputs[firstInputIndex + i])
                        val exprI = builder.toString()
                        builder.clear()
                        "$nameI=$exprI;\n"
                    }
                defineLocalVars(builder)
                val variables = typeValues.map { (k, v) ->
                    Variable(v.type, k)
                } + listOf(
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
                builder.addFragment(combineFStageCellShaded)
                shader = builder.create(getKey(), "cmb1")
            }

            override val currentShader: Shader get() = shader
        }.finish()

        shader = shader1
        val (shader2, typeValues) = shader1
        shader2.use()
        skybox.bind(shader2, "reflectionMap", Filtering.LINEAR, Clamping.CLAMP)
        for ((k, v) in typeValues) {
            v.bind(shader2, k)
        }
        return shader2
    }

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        timeRendering(name, timer) {
            val framebuffer = FBStack[name, width, height, 3, BufferQuality.FP_16, samples, DepthBufferType.NONE]
            useFrame(width, height, false, framebuffer, Renderer.copyRenderer) {
                renderPurely2 {
                    val shader = bindShader(pipeline.bakedSkybox?.getTexture0() ?: blackCube)
                    combineLighting(shader, applyToneMapping = getBoolInput(4))
                }
            }
            setOutput(1, Texture(framebuffer.getTexture0()))
        }
    }
}