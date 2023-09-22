package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugText(
    val position: Vector3d,
    val text: CharSequence,
    val color: Int = -1,
    val timeOfDeath: Long = Time.nanoTime + 5000 * MILLIS_TO_NANOS
) {
    constructor(position: Vector3d, text: CharSequence, color: Int, duration: Float) :
            this(position, text, color, Time.nanoTime + (duration * 1e9f).toLong())
}