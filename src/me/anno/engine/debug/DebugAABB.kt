package me.anno.engine.debug

import org.joml.AABBd

class DebugAABB(
    val bounds: AABBd,
    color: Int, timeOfDeath: Long = defaultTime()
) : DebugItem(color, timeOfDeath) {
    constructor(bounds: AABBd, color: Int, duration: Float) :
            this(bounds, color, timeByDuration(duration))
}