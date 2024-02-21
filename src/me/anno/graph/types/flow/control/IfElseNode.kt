package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput

class IfElseNode : FixedControlFlowNode("If-Else Branch", inputs, outputs) {

    init {
        setInput(0, false)
    }

    override fun execute(): NodeOutput {
        val condition = inputs[1].getValue() == true
        return getOutputNodes(if (condition) 0 else 1)
    }

    companion object {
        val inputs = listOf("Flow", beforeName, "Boolean", "Condition")
        val outputs = listOf("Flow", "If True", "Flow", "If False")
    }

}