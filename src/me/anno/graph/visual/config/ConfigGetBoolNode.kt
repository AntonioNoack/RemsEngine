package me.anno.graph.visual.config

import me.anno.config.DefaultConfig
import me.anno.graph.visual.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetBoolNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetBool", input, output) {

    override fun calculate() = config[
        getInput(0) as String,
        getBoolInput(1)
    ]

    companion object {
        private val input = listOf("String", "Name", "Boolean", "Default")
        private val output = listOf("Bool", "Value")
    }
}