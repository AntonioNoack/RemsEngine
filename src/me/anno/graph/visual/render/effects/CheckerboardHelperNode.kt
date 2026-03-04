package me.anno.graph.visual.render.effects

import me.anno.ecs.components.mesh.material.Materials
import me.anno.engine.ui.render.RenderState
import me.anno.graph.visual.actions.ActionNode

/**
 * Uses checkerboard rendering for improved shading performance.
 * */
class CheckerboardHelperNode : ActionNode(
    "CheckerboardHelper",
    listOf(
        "Int", "Width", "Int", "Height",
        "Float", "LOD Bias"
    ), listOf(
        "Int", "Width", "Int", "Height", "Int",
        "Target Width", "Int", "Target Height",
        "Int", "Samples"
    )
) {

    init {
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 0f)
    }

    override fun executeAction() {
        val w = getIntInput(1)
        val h = getIntInput(2)
        Materials.lodBias = getFloatInput(3)
        setOutput(1, w.shr(1) + 1)
        setOutput(2, h.shr(1) + 1)
        setOutput(3, w)
        setOutput(4, h)
        setOutput(5, 2)

        if (flipPattern) {
            // todo ideally, we'd only move on x-axis, but then we miss jitter for y :(,
            //  but when we move on y, too, our effect is gone; -> we maybe do have to flip...
            // this probably collides with advanced rendering techniques :(
            //  -> doesn't matter too much, advanced stuff is for powerful hardware, but this effect is for weak hardware
            RenderState.cameraMatrix
                .translateLocal(2f / w, 2f / h, 0f)
                .invert(RenderState.cameraMatrixInv)
        }

        // todo check the sample positions, whether the pattern is usable for us: we expect diagonals
        //  if incompatible, we must disable our logic...
        //  supported patterns: (0,3), (3,0), (1,2), (2,1)
    }

    companion object {

        // todo where is that visual 2Hz flicker coming from???
        //  when printing once per frame, it goes away... Ubuntu VSync issues???

        val flipPattern get() = false // Time.frameIndex.hasFlag(1)
    }
}