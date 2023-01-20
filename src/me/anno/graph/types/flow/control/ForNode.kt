package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

class ForNode : FixedControlFlowNode("For Loop", inputs, outputs) {

    override fun execute(graph: FlowGraph): NodeOutput {
        val inputs = inputs!!
        val startIndex = graph.getValue(inputs[1]) as Long
        val endIndex = graph.getValue(inputs[2]) as Long
        val increment = graph.getValue(inputs[3]) as Long
        val reversed = graph.getValue(inputs[4]) as Boolean
        if (startIndex != endIndex) {
            val running = getOutputNodes(0).others.mapNotNull { it.node }
            if (running.isNotEmpty()) {
                if (reversed) {
                    for (index in (startIndex - 1) downTo endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(index, 1)
                        graph.requestId()
                        // new id, because it's a new run, and we need to invalidate all previously calculated values
                        // theoretically it would be enough to just invalidate the ones in that subgraph
                        // we'd have to calculate that list
                        graph.executeNodes(running)
                    }
                } else {
                    for (index in startIndex until endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(index, 1)
                        graph.requestId()
                        // new id, because it's a new run, and we need to invalidate all previously calculated values
                        // theoretically it would be enough to just invalidate the ones in that subgraph
                        // we'd have to calculate that list
                        graph.executeNodes(running)
                    }
                }
            }// else done
        }
        return getOutputNodes(2)
    }

    companion object {
        val inputs = listOf(
            "Flow", beforeName,
            "Long", "Start Index",
            "Long", "End Index",
            "Long", "Step",
            "Boolean", "Descending"
        )
        val outputs = listOf(
            "Flow", "Loop Body",
            "Long", "Loop Index",
            "Flow", afterName
        )
    }

}