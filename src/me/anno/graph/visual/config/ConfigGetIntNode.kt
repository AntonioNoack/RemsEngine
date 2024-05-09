package me.anno.graph.visual.config

import me.anno.config.DefaultConfig
import me.anno.graph.visual.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetIntNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetInt", input, output) {

    override fun calculate() = config[
        getInput(0).toString(),
        getInput(1) as Long
    ]

    companion object {
        @JvmStatic
        private val input = listOf("String", "Name", "Long", "Default")

        @JvmStatic
        private val output = listOf("Long", "Value")
    }
}