package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

class ForNode : FixedControlFlowNode("For", listOf("Flow", "Long", "Long", "Long"), listOf("Flow", "Long", "Flow")) {

    override fun execute(graph: FlowGraph): NodeOutput {
        val inputs = inputs!!
        val startIndex = graph.getValue(inputs[1]) as Long
        val endIndex = graph.getValue(inputs[2]) as Long
        val increment = graph.getValue(inputs[3]) as Long
        val running = getOutputNodes(0).others.mapNotNull { it.node }
        if (running.isNotEmpty()) {
            for (index in startIndex until endIndex step increment) {
                if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                setOutput(index, 1)
                graph.requestId()
                // new id, because it's a new run, and we need to invalidate all previously calculated values
                // theoretically it would be enough to just invalidate the ones in that subgraph
                // we'd have to calculate that list
                graph.executeNodes(running)
            }
        }// else done
        return getOutputNodes(2)
    }

}