package me.anno.engine.debug

import org.joml.Vector3d

class DebugRay(
    val start: Vector3d,
    val direction: Vector3d,
    color: Int, timeOfDeath: Long = defaultTime()
) : DebugItem(color, timeOfDeath) {
    constructor(start: Vector3d, direction: Vector3d, color: Int, duration: Float) :
            this(start, direction, color, timeByDuration(duration))
}