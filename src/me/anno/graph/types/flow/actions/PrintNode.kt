package me.anno.graph.types.flow.actions

import org.apache.logging.log4j.LogManager

class PrintNode : ActionNode("Print", listOf("Any?", "Printed"), emptyList()) {

    override fun executeAction() {
        LOGGER.info(inputs!![1].getValue().toString())
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PrintNode::class)
    }

}