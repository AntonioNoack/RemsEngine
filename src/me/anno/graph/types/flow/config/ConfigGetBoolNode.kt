package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetBoolNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetBool", input, output) {

    override fun calculate() = config[
            getInput(0) as String,
            getInput(1) as Boolean
    ]

    override val className: String get() = "ConfigGetBoolNode"

    companion object {
        private val input = listOf("String", "Name", "Boolean", "Default")
        private val output = listOf("Bool", "Value")
    }
}