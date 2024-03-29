package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getIntInput
import me.anno.io.utils.StringMap

class ConfigGetIntNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetInt", input, output) {

    override fun calculate() = config[
        getInput(0).toString(),
        getIntInput(1)
    ]

    companion object {
        @JvmStatic
        private val input = listOf("String", "Name", "Int", "Default")

        @JvmStatic
        private val output = listOf("Int", "Value")
    }
}