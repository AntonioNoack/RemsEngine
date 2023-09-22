package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugRay(
    val start: Vector3d,
    val direction: Vector3d,
    val color: Int,
    val timeOfDeath: Long = Time.nanoTime + 5000 * MILLIS_TO_NANOS
){
    constructor(start: Vector3d, direction: Vector3d, color: Int, duration: Float) :
            this(start, direction, color, Time.nanoTime + (duration * 1e9f).toLong())
}