package me.anno.graph.visual.render.scene.utils

import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.deferred.DeferredLayerType
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
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.render.compiler.GraphShader
import me.anno.utils.types.Arrays.getOrPut
import me.anno.utils.types.Booleans.toInt
import kotlin.collections.iterator

class DeferredLightsShader
private constructor(
    inputs: List<NodeInput>, name: String,
    type: LightType, isInstanced: Boolean,
    graph: FlowGraph, firstInputIndex: Int
) : GraphCompiler(graph) {

    companion object {

        private val types = listOf(
            DeferredLayerType.NORMAL,
            DeferredLayerType.REFLECTIVITY,
            DeferredLayerType.TRANSLUCENCY,
            DeferredLayerType.SHEEN,
            DeferredLayerType.DEPTH
        )

        fun getLightShader(
            inputs: List<NodeInput>, name: String,
            type: LightType, isInstanced: Boolean,
            graph: FlowGraph, firstInputIndex: Int,
            shaders: Array<GraphShader?>
        ): Shader {
            val id = type.ordinal * 2 + isInstanced.toInt()
            val (shader, typeValues) = shaders.getOrPut(id) {
                DeferredLightsShader(inputs, name, type, isInstanced, graph, firstInputIndex)
                    .finish()
            }
            shader.use()
            for ((k, v) in typeValues) {
                v.bind(shader, k)
            }
            return shader
        }
    }

    private fun finalPositionStage(): ShaderStage {
        return ShaderStage(
            "r-light-f1", listOf(
                Variable(GLSLType.V2F, "uv"),
                Variable(GLSLType.V1F, "finalDepth"),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
            ) + depthVars,
            "finalPosition = depthToPosition(uv,finalDepth);\n"
        ).add(rawToDepth).add(depthToPosition)
    }

    private fun inputsForLightsStage(
        typeValues: Map<String, TypeValue>,
        extraVariables: List<Variable>,
        extraFunctions: StringBuilder,
        expressions: String
    ): ShaderStage {
        val variables = types.indices.map { i ->
            val typeI = GLSLType.floats[types[i].workDims - 1]
            val nameI = types[i].glslName
            Variable(typeI, nameI, VariableMode.OUT)
        } + typeValues.map { (k, v) -> Variable(v.type, k) } +
                extraVariables +
                listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT))


        return ShaderStage("r-light-f0", variables, expressions)
            .add(extraFunctions.toString())
    }

    private val shader: Shader

    init {

        val expressions = CopyInputsOrClear.buildExpression(builder, types, {
            expr(inputs[firstInputIndex + it])
        }, "if(finalDepth > 1e38) discard;\n") // sky doesn't need lighting

        defineLocalVars(builder)

        val builder = ShaderBuilder(name)
        builder.addVertex(if (isInstanced) vertexI else vertexNI)
        if (isInstanced) builder.addFragment(invStage)

        builder.addFragment(uvwStage)
        builder.addFragment(inputsForLightsStage(typeValues, extraVariables, extraFunctions, expressions))
        builder.addFragment(finalPositionStage())
        builder.addFragment(createMainFragmentStage(type, isInstanced))
        shader = builder.create(getKey(), "${type.ordinal}-${isInstanced.toInt()}")
    }

    override val currentShader: Shader get() = shader

    // not being used, as we only have an expression
    override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()
}