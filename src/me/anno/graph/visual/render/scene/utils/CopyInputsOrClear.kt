package me.anno.graph.visual.render.scene.utils

import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.BaseShader.Companion.getKey
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.render.compiler.GraphShader
import me.anno.graph.visual.render.scene.RenderDeferredNode.Companion.depthInputIndex
import me.anno.graph.visual.render.scene.RenderDeferredNode.Companion.firstInputIndex
import me.anno.graph.visual.render.scene.RenderDeferredNode.Companion.firstOutputIndex
import me.anno.graph.visual.render.scene.RenderDeferredNode.Companion.inList
import me.anno.graph.visual.render.scene.RenderViewNode.Companion.isOutputUsed
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Strings.iff
import kotlin.collections.iterator

class CopyInputsOrClear
private constructor(
    inputs: List<NodeInput>, outputs: List<NodeOutput>, name: String,
    graph: FlowGraph, renderer: Renderer
) : GraphCompiler(graph) {

    companion object {

        fun hasNonDepthInputs(inputs: List<NodeInput>): Boolean {
            val ignoredI = depthInputIndex - firstInputIndex
            for (i in 0 until inList.size.shr(1)) {
                if (i != ignoredI && !inputs[firstInputIndex + i].isEmpty()) {
                    return true
                }
            }
            return false
        }

        fun bindCopyShader(
            inputs: List<NodeInput>, outputs: List<NodeOutput>, name: String,
            graph: FlowGraph, shaders: HashMap<Renderer, GraphShader>
        ): Shader {
            val renderer = GFXState.currentRenderer
            val shader1 = shaders.getOrPut(renderer) {
                CopyInputsOrClear(inputs, outputs, name, graph, renderer).finish()
            }

            val (shader, typeValues) = shader1
            shader.use()
            for ((k, v) in typeValues) {
                v.bind(shader, k)
            }
            return shader
        }

        fun buildExpression(
            builder: StringBuilder, types: List<DeferredLayerType>,
            appendExpressionI: (Int) -> Unit, suffix: String
        ): String {
            assertTrue(builder.isEmpty())
            for (i in types.indices) {
                val nameI = types[i].glslName
                builder.append(nameI).append(" = ")
                appendExpressionI(i)
                builder.append(";\n")
            }
            builder.append(suffix)
            val result = builder.toString()
            builder.clear()
            return result
        }
    }

    private fun pipedInputs(inputs: List<NodeInput>, outputs: List<NodeOutput>): List<IndexedValue<DeferredLayerType>> {
        return DeferredLayerType.values.withIndex()
            .filter { (index, _) ->
                !inputs[index + firstInputIndex].isEmpty() &&
                        isOutputUsed(outputs[index + firstOutputIndex])
            }
    }

    private fun needsFinalId(outputs: List<IndexedValue<DeferredLayerType>>): Boolean {
        return outputs.any2 { (_, value) ->
            value == DeferredLayerType.CLICK_ID || value == DeferredLayerType.GROUP_ID
        }
    }

    private val shader: Shader

    init {

        // to do: filter for non-composite types
        // only load what is given? -> yes :D

        val output0 = pipedInputs(inputs, outputs)
        val outputs = renderer.deferredSettings!!.semanticLayers.toList()
            .map { tt -> output0.first { it.value == tt.type } }

        if (needsFinalId(outputs)) {
            extraVariables.add(Variable(GLSLType.V4F, "finalId", VariableMode.INOUT))
        }

        val writeDepth = outputs.any2 { it.value == DeferredLayerType.DEPTH }
        val expressions = buildExpression(builder, outputs.map { it.value }, {
            expr(inputs[firstInputIndex + outputs[it].index])
        }, "gl_FragDepth = depthToRaw(finalDepth);\n".iff(writeDepth))

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
                    Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
                    Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                ), "gl_Position = vec4(positions*2.0-1.0,0.0,1.0);\n" +
                        "uv = positions;\n"
            )
        )

        builder.settings = renderer.deferredSettings
        builder.useRandomness = false
        builder.addFragment(
            ShaderStage("rsdn-expr", variables, expressions)
                .add(extraFunctions.toString())
        )
        shader = builder.create(getKey(), "rsdn-${outputs.joinToString { it.value.name }}")
    }

    override val currentShader: Shader get() = shader

    // not being used, as we only have an expression
    override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()
}