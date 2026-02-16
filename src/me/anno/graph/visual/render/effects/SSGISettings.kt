package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting
import me.anno.utils.GFXFeatures

/**
 * Settings for SSGINode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class SSGISettings : Component(), GlobalSetting {

    var numSamples = 64
    var strength = 0.5f
    var radiusScale = 0.2f
    var blur = true

    override var priority = 0.0
}
