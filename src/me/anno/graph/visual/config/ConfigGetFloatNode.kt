package me.anno.graph.visual.config

import me.anno.config.DefaultConfig
import me.anno.graph.visual.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetFloatNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetFloat", input, output) {

    override fun calculate() = config[
        getInput(0) as String,
        getInput(1) as Double
    ]

    companion object {
        private val input = listOf("String", "Name", "Double", "Default")
        private val output = listOf("Double", "Value")
    }
}