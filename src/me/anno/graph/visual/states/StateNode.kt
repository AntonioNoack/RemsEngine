package me.anno.graph.visual.states

import me.anno.graph.visual.EarlyExitNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.NodeConnector

open class StateNode(
    name: String = "State",
    inputs: List<String> = emptyList(),
    outputs: List<String> = emptyList()
) : ActionNode(name, inputs, outputs), EarlyExitNode {

    open fun update(): StateNode {
        val graph = graph as? FlowGraph
        return graph?.executeNodes(outputs) as? StateNode ?: this
    }

    open fun onEnterState(oldState: StateNode?) {}
    open fun onExitState(newState: StateNode?) {}

    // = onDuringState()
    override fun executeAction() {}

    override fun canAddOutput(type: String, index: Int): Boolean = index > 0 && type == "Flow"
    override fun canRemoveOutput(type: String, index: Int): Boolean = index > 0 && type == "Flow"

    override fun supportsMultipleInputs(con: NodeConnector) = true
    override fun supportsMultipleOutputs(con: NodeConnector) = true
}