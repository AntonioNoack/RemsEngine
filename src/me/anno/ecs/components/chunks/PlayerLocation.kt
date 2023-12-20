package me.anno.ecs.components.chunks

open class PlayerLocation(
    var x: Double, var y: Double, var z: Double,
    var loadMultiplier: Double = 1.0,
    var unloadMultiplier: Double = 1.0
)