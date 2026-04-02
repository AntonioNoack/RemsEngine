package me.anno.graph.visual.render.effects

import me.anno.ecs.components.mesh.material.Materials
import me.anno.graph.visual.actions.ActionNode

/**
 * Helps improve texture quality when using TAA:
 *  if the camera is steady, TAA blurs the result, so we should use a LOD bias
 *  else, don't bias
 * */
class TAAHelperNode : ActionNode("TAAHelper", emptyList(), emptyList()) {
    override fun executeAction() {
        Materials.lodBias = -1f
        TAANode.getPattern(0, Materials.jitterInPixels)
    }
}