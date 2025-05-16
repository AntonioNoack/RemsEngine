package me.anno.graph.visual.render.effects

import me.anno.ecs.components.mesh.material.Materials
import me.anno.graph.visual.actions.ActionNode
import me.anno.maths.Maths.mix

/**
 * Helps improve texture quality when using TAA:
 *  if the camera is steady, TAA blurs the result, so we should use a LOD bias
 *  else, don't bias
 * */
class TAAHelperNode : ActionNode("TAAHelper", emptyList(), emptyList()) {
    override fun executeAction() {
        val s = TAANode.getCameraSteadiness()
        // if steady, return -1 to avoid blurring
        // else, return 0 for smooth textures
        Materials.lodBias = mix(0f, -1f, s) // = -s
        Materials.jitterInPixels.set(TAANode.getPattern(0))
    }
}