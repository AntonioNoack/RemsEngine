package me.anno.engine.debug

import me.anno.io.files.FileReference
import org.joml.Vector3d

class DebugIcon(
    val position: Vector3d,
    val source: FileReference,
    val size: Float,
    color: Int,
    timeOfDeath: Long = defaultTime(),
) : DebugItem(color, timeOfDeath) {
    constructor(position: Vector3d, source: FileReference, size: Float, color: Int, duration: Float) :
            this(position, source, size, color, timeByDuration(duration))
}