package me.anno.engine.ui.render.graph

import me.anno.graph.Node

class OutputNode : Node("Output", inputs, emptyList()) {
    companion object {
        private val inputs = listOf("Texture", "Texture")
    }
}