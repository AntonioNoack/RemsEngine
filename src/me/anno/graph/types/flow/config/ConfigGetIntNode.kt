package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetIntNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetInt", input, output) {

    override fun calculate(graph: FlowGraph) = config[
            getInput(graph, 0) as String,
            getInput(graph, 1) as Int
    ]

    override val className = "ConfigGetIntNode"

    companion object {
        private val input = listOf("String", "Name", "Int", "Default")
        private val output = listOf("Int", "Value")
    }
}