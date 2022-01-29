package me.anno.graph.types.flow

import me.anno.graph.Node
import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

abstract class FlowGraphNode : Node {

    constructor() : super()

    constructor(name: String) : super(name)

    constructor(name: String, inputs: List<String>, outputs: List<String>) :
            super(name, inputs, outputs)

    fun getInput(graph: FlowGraph, index: Int): Any? {
        return inputs!![index].castGetValue(graph, graph.validId)
    }

    abstract fun execute(graph: FlowGraph): NodeOutput?

    override val className: String = javaClass.simpleName

    companion object {
        val beforeName = "Before"
        val afterName = "After"
    }

}