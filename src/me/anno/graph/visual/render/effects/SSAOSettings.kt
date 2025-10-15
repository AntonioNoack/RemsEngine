package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting
import me.anno.utils.GFXFeatures

/**
 * Settings for SSAONode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class SSAOSettings : Component(), GlobalSetting {

    var numSamples = if (GFXFeatures.hasWeakGPU) 6 else 24
    var strength = 1f
    var radiusScale = 0.2f

    override var priority = 0.0
}
