package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.systems.GlobalSetting
import org.joml.Vector3f

/**
 * Settings for HeightExpFogNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class HeightExpFogSettings : Component(), GlobalSetting {

    @Range(1e-38, 1e38)
    var expFogDistance = 1000f

    @Range(0.0, 1.0)
    var heightFogStrength = 0.3f

    var heightFogSharpness = 1f

    var heightFogLevel = 0f

    var heightFogColor = Vector3f(0.375f, 0.491f, 0.697f)

    override var priority: Double = 0.0
}