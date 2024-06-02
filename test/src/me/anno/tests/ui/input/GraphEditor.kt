package me.anno.tests.ui.input

import me.anno.config.DefaultConfig
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.graph.visual.render.NodeGroup
import me.anno.ui.editor.graph.GraphEditor
import me.anno.ui.UIColors
import me.anno.ui.base.SpyPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Color.withAlpha

fun main() {
    ECSRegistry.init()
    val graph = RenderMode.DEFAULT.renderGraph!!
    val group = NodeGroup()
    group.name = "Sample Group"
    group.members.addAll(graph.nodes.subList(0, 2))
    group.extends.set(700.0, 300.0, 0.0)
    group.position.set(250.0, -40.0, 0.0)
    group.color = UIColors.darkOrange.withAlpha(0.5f)
    graph.groups.add(group)
    val editor = GraphEditor(graph, DefaultConfig.style)
    editor.weight = 1f
    val spy = SpyPanel {
        // performance test for generating lots of text
        val testTextPerformance = false
        if (testTextPerformance) {
            editor.scale.mul(1.02)
            if (editor.scale.x > 10.0) {
                editor.scale.set(1.0)
            }
            editor.targetScale.set(editor.scale)
            editor.invalidateLayout()
        } else editor.invalidateDrawing() // for testing normal performance
    }
    testUI("Graph Editor", listOf(spy, editor))
}