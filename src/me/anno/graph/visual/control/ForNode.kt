package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.FlowGraph

class ForNode : FixedControlFlowNode("For Loop", inputs, outputs) {

    override fun execute(): NodeOutput {
        val graph = graph as FlowGraph
        val startIndex = inputs[1].getValue() as Long
        val endIndex = inputs[2].getValue() as Long
        val increment = inputs[3].getValue() as Long
        val reversed = inputs[4].getValue() as Boolean
        if (startIndex != endIndex) {
            val running = getOutputNodes(0).others.mapNotNull { it.node }
            if (running.isNotEmpty()) {
                if (reversed) {
                    for (index in (startIndex - 1) downTo endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(1, index)
                        graph.requestId()
                        // new id, because it's a new run, and we need to invalidate all previously calculated values
                        // theoretically it would be enough to just invalidate the ones in that subgraph
                        // we'd have to calculate that list
                        graph.executeNodes(running)
                    }
                } else {
                    for (index in startIndex until endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(1, index)
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