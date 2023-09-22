package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugPoint(
    val position: Vector3d,
    val color: Int = -1,
    val timeOfDeath: Long = Time.nanoTime + 5000 * MILLIS_TO_NANOS
) {
    constructor(position: Vector3d, color: Int, duration: Float) :
            this(position, color, Time.nanoTime + (duration * 1e9f).toLong())
}