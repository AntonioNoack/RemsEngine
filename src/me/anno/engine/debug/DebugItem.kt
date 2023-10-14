package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS

abstract class DebugItem(val color: Int, val timeOfDeath: Long = defaultTime()) {
    companion object {
        fun timeByDuration(duration: Float) = Time.nanoTime + (duration * 1e9f).toLong()
        fun defaultTime() = Time.nanoTime + 5000 * MILLIS_TO_NANOS
    }
}