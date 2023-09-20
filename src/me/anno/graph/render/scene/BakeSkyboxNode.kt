package me.anno.graph.render.scene

import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.types.flow.actions.ActionNode

class BakeSkyboxNode : ActionNode("Render Scene", listOf("Int", "Resolution"), emptyList()) {

    init {
        setInput(1, 256) // default resolution
    }

    lateinit var pipeline: Pipeline

    override fun executeAction() {
        val resolution = getInput(1) as Int
        pipeline.bakeSkybox(resolution)
    }
}