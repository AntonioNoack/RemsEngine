package me.anno.graph.visual.render.effects

import me.anno.gpu.query.GPUClockNanos
import me.anno.graph.visual.actions.ActionNode

/**
 * A node with a GPU-timer, so the rendering times for it can be debugged easily.
 * */
abstract class TimedRenderingNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    // todo implement reasonable fail() for all rendering nodes

    val timer = GPUClockNanos()

    override fun destroy() {
        super.destroy()
        timer.destroy()
    }
}