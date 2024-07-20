package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.types.Floats.toLongOr

abstract class DebugItem(val color: Int, val timeOfDeath: Long = defaultTime()) {
    companion object {
        fun timeByDuration(duration: Float): Long = Time.nanoTime + (duration * 1e9f).toLongOr()
        fun defaultTime() = Time.nanoTime + 5000 * MILLIS_TO_NANOS
    }
}