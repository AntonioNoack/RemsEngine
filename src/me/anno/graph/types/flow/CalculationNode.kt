package me.anno.graph.types.flow

import me.anno.graph.types.FlowGraph

abstract class CalculationNode : ValueNode {

    constructor() : super()

    constructor(inputTypes: List<String>, outputTypes: List<String>) : super(inputTypes, outputTypes)

    constructor(inputType: String, inputCount: Int, outputType: String, outputCount: Int) :
            this(Array(inputCount) { inputType }.toList(), Array(outputCount) { outputType }.toList())

    abstract fun calculate(graph: FlowGraph): Any?

    override fun compute(graph: FlowGraph) {
        setOutput(calculate(graph), 0)
    }

}