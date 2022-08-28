package me.anno.graph.render

import me.anno.graph.Node

class OutputNode : Node("Output", inputs, emptyList()) {
    companion object {
        private val inputs = listOf("Texture", "Result")
    }
}