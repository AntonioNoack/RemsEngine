package me.anno.tests.graph.visual

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.render.RenderReturnNode
import me.anno.graph.visual.render.RenderGraphEditor
import me.anno.sdf.shapes.SDFSphere
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    OfficialExtensions.initForTests()
    val rv = RenderView1(PlayMode.EDITING, SDFSphere(), style)
    val graph = FlowGraph()
    val start = graph.add(StartNode(listOf("Int", "Width", "Int", "Height")))
    val ret = graph.add(RenderReturnNode())
    start.connectTo(ret)
    testUI3("ShaderExprNode", RenderGraphEditor(rv, graph, style))
}