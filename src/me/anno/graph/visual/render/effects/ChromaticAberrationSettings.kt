package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.systems.GlobalSetting
import org.joml.Vector2f

/**
 * Settings for ChromaticAberrationNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class ChromaticAberrationSettings : Component(), GlobalSetting {

    var strength = 1f
    var power = 1.5f

    var rOffset = Vector2f()
    var bOffset = Vector2f()

    override var priority = 0.0
}