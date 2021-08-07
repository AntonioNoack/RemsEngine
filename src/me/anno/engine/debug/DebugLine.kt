package me.anno.engine.debug

import me.anno.gpu.GFX
import org.joml.Vector3d

class DebugLine(val p0: Vector3d, val p1: Vector3d, val color: Int, val timeOfDeath: Long = GFX.gameTime + 5e9.toLong()) {
}