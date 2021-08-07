package me.anno.engine.debug

import me.anno.gpu.GFX
import org.joml.Vector3d

class DebugPoint(val position: Vector3d, val color: Int = -1, val timeOfDeath: Long = GFX.gameTime + 5e9.toLong())