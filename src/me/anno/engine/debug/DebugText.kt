package me.anno.engine.debug

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugText(
    val position: Vector3d,
    val text: CharSequence,
    val color: Int = -1,
    val timeOfDeath: Long = Engine.gameTime + 5000 * MILLIS_TO_NANOS
) {
    constructor(position: Vector3d, text: CharSequence, color: Int, duration: Float) :
            this(position, text, color, Engine.gameTime + (duration * 1e9f).toLong())
}