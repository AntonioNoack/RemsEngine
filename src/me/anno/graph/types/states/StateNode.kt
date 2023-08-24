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

    open fun update(): StateNode {
        return try {
            val outputs = outputs
            val graph = graph as? FlowGraph
            if (outputs != null && graph != null) {
                for (output in outputs) {
                    for (input in output.others) {
                        val node = input.node
                        if (node != null) {
                            graph.execute(node)
                        }
                    }
                }
            }
            this
        } catch (e: NewState) {
            e.state
        }
    }

    open fun onEnterState(oldState: StateNode?) {}

    open fun onExitState(newState: StateNode?) {}

    final override fun executeAction() {
        throw NewState(this)
    }

    override fun canAddOutput(type: String, index: Int): Boolean = type == "Flow"
    override fun supportsMultipleInputs(con: NodeConnector) = true
    override fun supportsMultipleOutputs(con: NodeConnector) = true
}