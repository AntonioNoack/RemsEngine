package me.anno.graph.types.flow

abstract class CalculationNode : ValueNode {

    @Suppress("unused")
    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputType: String) :
            super(name, inputs, listOf(outputType, "Result"))

    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)

    abstract fun calculate(): Any?

    override fun compute() {
        setOutput(calculate(), 0)
    }

}