package me.anno.graph.render

import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.RenderGraph.startArguments
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.StartNode

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

    private fun placeNode(node: Node) {
        node.position.set(350.0 * graph.nodes.size, 0.0, 0.0)
        node.graph = graph
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

    private fun registerOutput(output: NodeOutput, extraOutputs: Map<String, List<String>>) {
        val mapping = extraOutputs[output.name]
        if (mapping != null) {
            for (name in mapping) {
                values[name] = output
            }
        } else values[output.name] = output
    }

    fun finish(end: ExprReturnNode = ExprReturnNode()) = then(end).graph
    fun finish(extraInputs: Map<String, Any?>, end: ExprReturnNode = ExprReturnNode()) =
        then(end, extraInputs, emptyMap()).graph
}