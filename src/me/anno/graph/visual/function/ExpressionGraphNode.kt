package me.anno.graph.visual.function

import me.anno.graph.visual.CalculationNode

class ExpressionGraphNode(name: String, inputs: List<String>, outputs: List<String>) :
    CalculationNode(name, inputs, outputs) {
    override fun calculate(): Any? {
        TODO("Not yet implemented")
    }
}