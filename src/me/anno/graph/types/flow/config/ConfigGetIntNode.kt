package me.anno.graph.types.flow.config

import me.anno.config.DefaultConfig
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.utils.StringMap

class ConfigGetIntNode(val config: StringMap = DefaultConfig) :
    CalculationNode("Config.GetInt", input, output) {

    override fun calculate() = config[
            getInput(0) as String,
            getInput(1) as Int
    ]

    override val className get() = "ConfigGetIntNode"

    companion object {
        @JvmStatic
        private val input = listOf("String", "Name", "Int", "Default")

        @JvmStatic
        private val output = listOf("Int", "Value")
    }
}