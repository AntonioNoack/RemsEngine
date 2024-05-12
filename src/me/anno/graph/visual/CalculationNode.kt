package me.anno.graph.visual

/**
 * ComputeNode with a single output
 * */
abstract class CalculationNode : ComputeNode {

    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)
    constructor(name: String, inputs: List<String>, outputType: String) :
            super(name, inputs, listOf(outputType, "Result"))

    abstract fun calculate(): Any?

    final override fun compute() {
        setOutput(0, calculate())
    }
}