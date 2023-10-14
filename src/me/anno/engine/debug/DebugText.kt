package me.anno.engine.debug

import org.joml.Vector3d

class DebugText(
    val position: Vector3d,
    val text: CharSequence,
    color: Int,
    timeOfDeath: Long = defaultTime()
) : DebugItem(color, timeOfDeath) {
    constructor(position: Vector3d, text: CharSequence, color: Int, duration: Float) :
            this(position, text, color, timeByDuration(duration))
}