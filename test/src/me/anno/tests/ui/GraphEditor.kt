package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.engine.ECSRegistry
import me.anno.graph.render.NodeGroup
import me.anno.graph.types.FlowGraph
import me.anno.graph.ui.GraphEditor
import me.anno.ui.base.SpyPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    ECSRegistry.init()
    val graph = FlowGraph.testLocalVariables()
    val group = NodeGroup()
    group.members.addAll(graph.nodes.subList(0, 2))
    group.extends.set(500.0, 500.0, 0.0)
    graph.groups.add(group)
    val editor = GraphEditor(graph, DefaultConfig.style)
    val spy = SpyPanel {
        // performance test for generating lots of text
        val testTextPerformance = false
        if (testTextPerformance) {
            editor.scale *= 1.02
            if (editor.scale > 10.0) {
                editor.scale = 1.0
            }
            editor.targetScale = editor.scale
            editor.invalidateLayout()
        } else editor.invalidateDrawing() // for testing normal performance
    }
    testUI(listOf(spy, editor))
}