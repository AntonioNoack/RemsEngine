package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

class WhileNode : FixedControlFlowNode(1, listOf("Boolean"), 2, emptyList()) {

    override fun execute(graph: FlowGraph): NodeOutput {
        val running = getOutputNodes(0).others.mapNotNull { it.node }
        while (true) {
            val condition = graph.getValue(inputs!![1]) != false
            if (!condition) break
            if (running.isNotEmpty()) {
                graph.requestId()
                // new id, because it's a new run, and we need to invalidate all previously calculated values
                // theoretically it would be enough to just invalidate the ones in that subgraph
                // we'd have to calculate that list
                for (node in running) {
                    graph.execute(node)
                }
            } else Thread.sleep(1) // wait until condition does false
        }
        return getOutputNodes(1)
    }

}