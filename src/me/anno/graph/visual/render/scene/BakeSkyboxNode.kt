package me.anno.graph.visual.render.scene

import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.visual.FlowGraphNodeUtils.getIntInput
import me.anno.graph.visual.actions.ActionNode

/**
 * bakes the skybox (for reflections and ambient light)
 * */
class BakeSkyboxNode : ActionNode("Render Scene", listOf("Int", "Resolution"), emptyList()) {

    init {
        setInput(1, 256) // default resolution
    }

    lateinit var pipeline: Pipeline

    override fun executeAction() {
        val resolution = getIntInput(1)
        pipeline.bakeSkybox(resolution)
    }
}