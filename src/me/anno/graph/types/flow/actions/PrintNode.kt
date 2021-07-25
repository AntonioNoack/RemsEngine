package me.anno.graph.types.flow.actions

import me.anno.graph.types.FlowGraph
import org.apache.logging.log4j.LogManager

class PrintNode : ActionNode(listOf("Any?"), listOf()) {

    override fun executeAction(graph: FlowGraph) {
        LOGGER.info(graph.getValue(inputs!![1]).toString())
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PrintNode::class)
    }

}