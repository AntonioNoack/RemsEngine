package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.systems.GlobalSetting

/**
 * Settings for DepthOfFieldNode.
 * Just add them to your scene, and you can control it.
 *
 * The setting with the highest priority gets selected if multiple components are available.
 * */
class DepthOfFieldSettings : Component(), GlobalSetting {

    var focusPoint = 1f
    var focusScale = 0.25f

    /**
     * Smaller = nicer blur, larger = faster
     * */
    @Range(0.25, 2.0)
    var radScale = 0.5f

    /**
     * in pixels
     * */
    @Range(1.0, 20.0)
    var maxBlurSize = 20f

    var spherical = 0f

    override var priority = 0.0
}