package me.anno.graph.visual.config

import me.anno.config.DefaultConfig
import me.anno.graph.visual.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetIntNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetInt", input, output) {

    override fun calculate() = config[
        getInput(0).toString(),
        getLongInput(1)
    ]

    companion object {
        @JvmStatic
        private val input = listOf("String", "Name", "Long", "Default")

        @JvmStatic
        private val output = listOf("Long", "Value")
    }
}