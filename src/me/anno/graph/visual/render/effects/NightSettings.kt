package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting

/**
 * Settings for NightNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class NightSettings : Component(), GlobalSetting {

    var strength = 1f
    var skyBrightnessFactor = 0.01f

    override var priority = 0.0
}
