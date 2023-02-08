package me.anno.graph.types.flow

import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeOutput

abstract class FlowGraphNode : Node {

    @Suppress("unused")
    constructor() : super()

    constructor(name: String) : super(name)

    constructor(name: String, inputs: List<String>, outputs: List<String>) :
            super(name, inputs, outputs)

    fun getInput(index: Int): Any? {
        return inputs!![index].getValue()
    }

    abstract fun execute(): NodeOutput?

    override fun supportsMultipleInputs(con: NodeConnector): Boolean {
        return con.type == "Flow"
    }

    override fun supportsMultipleOutputs(con: NodeConnector): Boolean {
        return con.type != "Flow"
    }

    companion object {
        val beforeName = "Before"
        val afterName = "After"
    }

}