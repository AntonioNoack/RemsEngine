package me.anno.graph.render.effects

import me.anno.graph.types.flow.actions.ActionNode
import me.anno.maths.Maths.clamp

/**
 * FSR reduces the rendering resolution, and then upscales that image.
 * This helper saves the original resolution for the reconstruction.
 * */
class FSR1HelperNode : ActionNode(
    "FSR1Helper",
    listOf("Int", "Width", "Int", "Height", "Float", "Fraction"),
    listOf("Int", "Width", "Int", "Height", "Int", "Target Width", "Int", "Target Height")
) {

    init {
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 0.7f)
    }

    override fun executeAction() {
        val w = getInput(1) as Int
        val h = getInput(2) as Int
        val f = clamp(getInput(3) as Float)
        setOutput(1, (w * f).toInt())
        setOutput(2, (h * f).toInt())
        setOutput(3, w)
        setOutput(4, h)
    }
}