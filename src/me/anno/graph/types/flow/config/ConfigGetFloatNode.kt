package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
import me.anno.io.utils.StringMap

class ConfigGetFloatNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetFloat", input, output) {

    override fun calculate() = config[
        getInput(0) as String,
        getFloatInput(1)
    ]

    companion object {
        private val input = listOf("String", "Name", "Float", "Default")
        private val output = listOf("Float", "Value")
    }
}