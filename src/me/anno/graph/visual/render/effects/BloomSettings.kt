package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.systems.GlobalSetting

/**
 * Settings for BloomNode.
 * Just add them to your scene, and you can control it.
 *
 * The setting with the highest priority gets selected if multiple components are available.
 * */
class BloomSettings : Component(), GlobalSetting {

    var offset = 0f
    var strength = 0.001f

    override var priority = 0.0
}