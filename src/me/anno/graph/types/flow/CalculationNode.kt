package me.anno.graph.types.flow

import me.anno.graph.types.FlowGraph

abstract class CalculationNode : ValueNode {

    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)

    abstract fun calculate(graph: FlowGraph): Any?

    override fun compute(graph: FlowGraph) {
        setOutput(calculate(graph), 0)
    }

}