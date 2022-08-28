package me.anno.graph.render

import me.anno.engine.ui.render.RenderView
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode

class SceneNode : CalculationNode("Scene", emptyList(), outputs) {

    override fun calculate(graph: FlowGraph): Any? {
        return RenderView.currentInstance!!.getWorld()
    }

    companion object {
        private val outputs = listOf("PrefabSaveable", "Scene")
    }

}