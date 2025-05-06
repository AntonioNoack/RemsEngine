package me.anno.graph.visual.render

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.RenderGraph.startArguments
import kotlin.math.max

/**
 * quickly creates pipelines for RenderModes
 *
 * todo right click on node in editor -> "then" -> choose node -> prefill all inputs :)
 * */
class QuickPipeline {

    val values = HashMap<String, NodeOutput>()
    val graph = FlowGraph()
    val start = StartNode(startArguments)

    init {
        then(start)
    }

    fun then(node: Node): QuickPipeline {
        return then(node, emptyMap(), emptyMap())
    }

    fun then1(node: Node, extraInputs: Map<String, Any?>): QuickPipeline {
        return then(node, extraInputs, emptyMap())
    }

    fun then(node: Node, extraOutputs: Map<String, List<String>>): QuickPipeline {
        return then(node, emptyMap(), extraOutputs)
    }

    fun then(node: Node, extraInputs: Map<String, Any?>, extraOutputs: Map<String, List<String>>): QuickPipeline {
        connectFlow(node)
        placeNode(node)
        graph.nodes.add(node)
        connectInputs(node, extraInputs)
        registerOutputs(node, extraOutputs)
        return this
    }

    private fun connectFlow(node: Node) {
        if (node.inputs.firstOrNull()?.type == "Flow") {
            graph.nodes.lastOrNull {
                it.outputs.firstOrNull()?.type == "Flow"
            }?.connectTo(0, node, 0)
        }
    }

    private var graphWidth = 0.0
    private val nodeSpacing get() = 25.0
    private fun placeNode(node: Node) {
        val width = estimateNodeWidth(node)
        node.position.set(graphWidth + width * 0.5, 0.0, 0.0)
        node.graph = graph
        graphWidth += width + nodeSpacing
    }

    private fun estimateNodeWidth(node: Node): Double {
        var maxLength = node.name.length
        val inputs = node.inputs
        val outputs = node.outputs
        for (i in 0 until max(inputs.size, outputs.size)) {
            val input = inputs.getOrNull(i)
            val inputName = input?.name ?: ""
            val inputValue = if (input != null && hasUI(input.type))
                input.getValue().toString() else ""
            val outputName = outputs.getOrNull(i)?.name ?: ""
            maxLength = max(maxLength, inputName.length + inputValue.length + outputName.length)
        }
        return maxLength * 18.0
    }

    private fun hasUI(type: String): Boolean {
        return when (type) {
            "Int", "Long", "Float", "Double", "Bool", "Boolean" -> true
            else -> type.startsWith("Enum<")
        }
    }

    private fun connectInputs(node: Node, extraInputs: Map<String, Any?>) {
        val inputs = node.inputs
        for (i in inputs.indices) {
            val input = inputs[i]
            if (input.type != "Flow") {
                connectInput(node, input, i, extraInputs)
            }
        }
    }

    private fun connectInput(node: Node, input: NodeInput, i: Int, extraInputs: Map<String, Any?>) {
        if (input.name in extraInputs) {
            node.setInput(i, extraInputs[input.name])
        } else {
            val source = values[input.name]
            if (source != null) {
                input.connect(source)
            }
        }
    }

    private fun registerOutputs(node: Node, extraOutputs: Map<String, List<String>>) {
        val outputs = node.outputs
        for (i in outputs.indices) {
            val output = outputs[i]
            if (output.type != "Flow") {
                registerOutput(output, extraOutputs)
            }
        }
    }

    fun mapOutputs(extraOutputs: Map<String, List<String>>): QuickPipeline {
        registerOutputs(graph.nodes.last(), extraOutputs)
        return this
    }

    private fun registerOutput(output: NodeOutput, extraOutputs: Map<String, List<String>>) {
        val mapping = extraOutputs[output.name]
        if (mapping != null) {
            for (name in mapping) {
                values[name] = output
            }
        } else values[output.name] = output
    }

    fun finish(end: RenderReturnNode = RenderReturnNode()) = then(end).graph
    fun finish(extraInputs: Map<String, Any?>, end: RenderReturnNode = RenderReturnNode()) =
        then(end, extraInputs, emptyMap()).graph
}