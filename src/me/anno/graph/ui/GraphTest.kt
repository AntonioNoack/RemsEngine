package me.anno.graph.ui

import me.anno.config.DefaultConfig.style
import me.anno.graph.types.FlowGraph
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    testUI {
        val g = FlowGraph.testLocalVariables()
        g.calculateNodePositions()
        GraphPanel(g, style)
    }
}