package me.anno.engine.debug

import org.joml.Vector3d

class DebugArrow(
    val p0: Vector3d, val p1: Vector3d,
    val headSize: Float,
    color: Int, timeOfDeath: Long = defaultTime(),
) : DebugItem(color, timeOfDeath) {
    val from get() = p0
    val to get() = p1

    constructor(p0: Vector3d, p1: Vector3d, headSize: Float, color: Int, duration: Float) :
            this(p0, p1, headSize, color, timeByDuration(duration))

    constructor(p0: Vector3d, p1: Vector3d, color: Int, timeOfDeath: Long = defaultTime()) :
            this(p0, p1, defaultHeadSize(p0, p1), color, timeOfDeath)

    constructor(p0: Vector3d, p1: Vector3d, color: Int, duration: Float) :
            this(p0, p1, defaultHeadSize(p0, p1), color, timeByDuration(duration))

    companion object {
        fun defaultHeadSize(p0: Vector3d, p1: Vector3d): Float {
            return p0.distance(p1).toFloat() * 0.2f
        }
    }
}