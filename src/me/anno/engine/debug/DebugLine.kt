package me.anno.engine.debug

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugLine(
    val p0: Vector3d,
    val p1: Vector3d,
    val color: Int,
    val timeOfDeath: Long = Engine.gameTime + 5000 * MILLIS_TO_NANOS
)