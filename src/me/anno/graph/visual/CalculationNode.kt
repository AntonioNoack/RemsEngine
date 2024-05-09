package me.anno.graph.visual

abstract class CalculationNode : ComputeNode {

    @Suppress("unused")
    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputType: String) :
            super(name, inputs, listOf(outputType, "Result"))

    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)

    abstract fun calculate(): Any?

    override fun compute() {
        setOutput(0, calculate())
    }
}