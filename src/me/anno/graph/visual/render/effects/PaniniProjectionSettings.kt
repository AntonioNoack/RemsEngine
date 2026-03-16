package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting
import me.anno.utils.GFXFeatures

/**
 * Settings for PaniniProjectionNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class PaniniProjectionSettings : Component(), GlobalSetting {

    var distance = 0.1f

    override var priority = 0.0
}
