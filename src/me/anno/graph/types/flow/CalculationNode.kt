package me.anno.graph.types.flow

import me.anno.graph.types.FlowGraph

abstract class CalculationNode : ValueNode {

    constructor(name: String) : super(name)

    constructor(name: String, inputTypes: List<String>, outputTypes: List<String>) : super(name, inputTypes, outputTypes)

    constructor(name: String, inputType: String, inputCount: Int, outputType: String, outputCount: Int) :
            this(name, Array(inputCount) { inputType }.toList(), Array(outputCount) { outputType }.toList())

    abstract fun calculate(graph: FlowGraph): Any?

    override fun compute(graph: FlowGraph) {
        setOutput(calculate(graph), 0)
    }

}