package me.anno.graph.visual.render.effects

import me.anno.gpu.query.GPUClockNanos
import me.anno.graph.visual.actions.ActionNode

abstract class TimedRenderingNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    val timer = GPUClockNanos()

    override fun destroy() {
        super.destroy()
        timer.destroy()
    }
}