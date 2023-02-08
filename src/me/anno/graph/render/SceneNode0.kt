package me.anno.graph.render

import me.anno.engine.ui.render.RenderView
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode

class SceneNode0 : CalculationNode("Scene", emptyList(), outputs) {

    override fun calculate(): Any? {
        return RenderView.currentInstance!!.getWorld()
    }

    companion object {
        @JvmStatic
        private val outputs = listOf("PrefabSaveable", "Scene")
    }

}