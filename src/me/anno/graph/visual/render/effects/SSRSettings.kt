package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting

/**
 * Settings for SSRNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class SSRSettings : Component(), GlobalSetting {

    var strength = 1f
    var maskSharpness = 1f
    var wallThickness = 0.2f
    var fineSteps = 10

    override var priority = 0.0
}