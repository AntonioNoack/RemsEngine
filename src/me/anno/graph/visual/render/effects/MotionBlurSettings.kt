package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.systems.GlobalSetting

/**
 * Settings for MotionBlurNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class MotionBlurSettings : Component(), GlobalSetting {

    @Range(1.0, 256.0)
    var maxSamples = 16

    @Range(0.01, 1.0)
    var shutter = 0.5f

    override var priority = 0.0
}