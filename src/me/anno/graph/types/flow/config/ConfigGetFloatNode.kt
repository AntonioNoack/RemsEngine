package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetFloatNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetFloat", input, output) {

    override fun calculate(graph: FlowGraph) = config[
            getInput(graph, 0) as String,
            getInput(graph, 1) as Float
    ]

    override val className = "ConfigGetFloatNode"

    companion object {
        private val input = listOf("String", "Name", "Float", "Default")
        private val output = listOf("Float", "Value")
    }
}