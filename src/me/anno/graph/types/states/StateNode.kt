package me.anno.graph.types.states

import me.anno.graph.NodeConnector
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode

open class StateNode(
    name: String = "State",
    inputs: List<String> = Companion.inputs,
    outputs: List<String> = Companion.outputs
) : ActionNode(name, inputs, outputs) {

    companion object {
        private val inputs = listOf("Flow", "Input")
        private val outputs = listOf("Flow", "OnUpdate")
    }

    open fun update(graph: FlowGraph): StateNode {
        return try {
            val outputs = outputs ?: return this
            for (output in outputs) {
                for (input in output.others) {
                    (input.node as? ActionNode)
                        ?.execute(graph)
                }
            }
            this
        } catch (e: NewState) {
            e.state
        }
    }

    override fun executeAction(graph: FlowGraph) {
        throw NewState(this)
    }

    override fun canAddOutput(type: String) = (type == "Flow")
    override fun supportsMultipleInputs(con: NodeConnector) = true
    override fun supportsMultipleOutputs(con: NodeConnector) = true

}