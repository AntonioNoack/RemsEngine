package me.anno.graph.types.flow

import me.anno.graph.NodeOutput

abstract class ComputeNode : FlowGraphNode {

    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)
    constructor(name: String, inputs: List<String>, outputType: String) : this(
        name, inputs, listOf(outputType, "Result")
    )

    abstract fun compute()

    override fun execute(): NodeOutput? {
        compute()
        return null
    }

}