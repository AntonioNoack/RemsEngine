package me.anno.recast

import me.anno.ecs.prefab.PrefabSaveable

class AgentType : PrefabSaveable() {

    var height = 2f
    var radius = 0.6f

    var maxStepHeight = 1.9f // "maxClimb"
    var maxSlopeDegrees = 45f

    var maxSpeed: Float = 1f
    var maxAcceleration: Float = 1f

}