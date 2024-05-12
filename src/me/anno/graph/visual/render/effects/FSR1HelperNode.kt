package me.anno.graph.visual.render.effects

import me.anno.graph.visual.actions.ActionNode
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
        val w = getIntInput(1)
        val h = getIntInput(2)
        val f = clamp(getFloatInput(3))
        setOutput(1, (w * f).toInt())
        setOutput(2, (h * f).toInt())
        setOutput(3, w)
        setOutput(4, h)
    }
}