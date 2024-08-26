package me.anno.engine.debug

import org.joml.Vector3d

class DebugLine(
    val p0: Vector3d, val p1: Vector3d,
    color: Int, timeOfDeath: Long = defaultTime()
) : DebugItem(color, timeOfDeath) {
    val from get() = p0
    val to get() = p1
    constructor(p0: Vector3d, p1: Vector3d, color: Int, duration: Float) :
            this(p0, p1, color, timeByDuration(duration))
}