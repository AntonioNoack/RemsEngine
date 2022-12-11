package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetBoolNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetBool", input, output) {

    override fun calculate(graph: FlowGraph) = config[
            getInput(graph, 0) as String,
            getInput(graph, 1) as Boolean
    ]

    override val className get() = "ConfigGetBoolNode"

    companion object {
        private val input = listOf("String", "Name", "Bool", "Default")
        private val output = listOf("Bool", "Value")
    }
}