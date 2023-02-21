package me.anno.engine.debug

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugPoint(
    val position: Vector3d,
    val color: Int = -1,
    val timeOfDeath: Long = Engine.gameTime + 5000 * MILLIS_TO_NANOS
) {
    constructor(position: Vector3d, color: Int, duration: Float) :
            this(position, color, Engine.gameTime + (duration * 1e9f).toLong())
}