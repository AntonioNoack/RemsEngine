package me.anno.engine.debug

import me.anno.gpu.GFX
import org.joml.Vector3d

class DebugRay(val start: Vector3d, val direction: Vector3d, val color: Int, val timeOfDeath: Long = GFX.gameTime + 5e9.toLong())