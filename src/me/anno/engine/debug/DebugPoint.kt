package me.anno.engine.debug

import org.joml.Vector3d

class DebugPoint(
    val position: Vector3d,
    color: Int, timeOfDeath: Long = defaultTime()
) : DebugItem(color, timeOfDeath) {
    @Suppress("unused")
    constructor(position: Vector3d, color: Int, duration: Float) :
            this(position, color, timeByDuration(duration))
}