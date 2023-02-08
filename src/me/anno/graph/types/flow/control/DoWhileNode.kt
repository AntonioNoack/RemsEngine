package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

class DoWhileNode : FixedControlFlowNode("Do While Loop", WhileNode.inputs, WhileNode.outputs) {

    override fun execute(): NodeOutput {
        val graph = graph as FlowGraph
        val running = getOutputNodes(0).others.mapNotNull { it.node }
        val condition0 = inputs!![1]
        while (true) {
            if (running.isNotEmpty()) {
                graph.requestId()
                // new id, because it's a new run, and we need to invalidate all previously calculated values
                // theoretically it would be enough to just invalidate the ones in that subgraph
                // we'd have to calculate that list
                for (node in running) {
                    graph.execute(node)
                }
            } else Thread.sleep(1) // wait until condition does false
            val condition = condition0.getValue() != false
            if (!condition) break
        }
        return getOutputNodes(1)
    }

}