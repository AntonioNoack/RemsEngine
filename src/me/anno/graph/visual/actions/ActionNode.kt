package me.anno.graph.visual.actions

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.control.FixedControlFlowNode

abstract class ActionNode : FixedControlFlowNode {

    constructor(name: String) : super(name, Companion.inputs, Companion.outputs)

    constructor(name: String, inputs: List<String>, outputs: List<String>) :
            super(name, Companion.inputs + inputs, Companion.outputs + outputs)

    abstract fun executeAction()

    /**
     * executes and returns first output node
     * */
    override fun execute(): NodeOutput {
        executeAction()
        return getNodeOutput(0)
    }

    companion object {
        val inputs = listOf("Flow", beforeName)
        val outputs = listOf("Flow", afterName)
    }

}