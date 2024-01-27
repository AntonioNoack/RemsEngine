package me.anno.graph.render

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.Node
import me.anno.graph.NodeOutput
import me.anno.graph.render.RenderGraph.startArguments
import me.anno.graph.render.scene.RenderSceneDeferredNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.StartNode

/**
 * quickly creates pipelines for existing RenderModes
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

        // connect flow, if available
        if (node.inputs.firstOrNull()?.type == "Flow") {
            graph.nodes.lastOrNull {
                it.outputs.firstOrNull()?.type == "Flow"
            }?.connectTo(0, node, 0)
        }

        // set node position
        node.position.set(350.0 * graph.nodes.size, 0.0, 0.0)
        node.graph = graph
        graph.nodes.add(node)

        // connect all inputs
        val inputs = node.inputs
        for (i in inputs.indices) {
            val input = inputs[i]
            if (input.type != "Flow") {
                if (input.name in extraInputs) {
                    node.setInput(i, extraInputs[input.name])
                } else {
                    val source = values[input.name]
                    if (source != null) {
                        input.connect(source)
                    }
                }
            }
        }

        // register all outputs
        for (output in node.outputs) {
            if (output.type != "Flow") {
                val mapping = extraOutputs[output.name]
                if (mapping != null) {
                    for (name in mapping) {
                        values[name] = output
                    }
                } else values[output.name] = output
            }
        }
        return this
    }

    fun render(target: DeferredLayerType): QuickPipeline {
        return then(
            RenderSceneDeferredNode(), mapOf(
                DeferredLayerType.COLOR.name to emptyList(),
                target.name to listOf("Color")
            )
        )
    }

    fun finish(end: ExprReturnNode = ExprReturnNode()) = then(end).graph
    fun finish(extraInputs: Map<String, Any?>, end: ExprReturnNode = ExprReturnNode()) =
        then(end, extraInputs, emptyMap()).graph

}