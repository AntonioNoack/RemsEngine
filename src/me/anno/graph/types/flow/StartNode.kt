package me.anno.graph.types.flow

import me.anno.graph.NodeOutput

open class StartNode(funcArguments: List<String> = emptyList()) :
    FlowGraphNode("Start", emptyList(), flow + funcArguments) {

    override fun execute(): NodeOutput? {
        return outputs?.getOrNull(0)
    }

    companion object {
        private val flow = listOf("Flow", "Start")
    }

}