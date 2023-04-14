package me.anno.graph.render.scene

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.types.flow.actions.ActionNode

abstract class RenderSceneNode0(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    lateinit var renderView: RenderView
    lateinit var pipeline: Pipeline

    abstract fun invalidate()

    override fun onDestroy() {
        super.onDestroy()
        invalidate()
    }

}