package me.anno.engine.debug

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.joml.Vector3d

class DebugRay(
    val start: Vector3d,
    val direction: Vector3d,
    val color: Int,
    val timeOfDeath: Long = Engine.gameTime + 5000 * MILLIS_TO_NANOS
)