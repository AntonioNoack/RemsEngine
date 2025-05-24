package me.anno.engine.debug

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.types.Floats.toLongOr
import kotlin.math.min

abstract class DebugItem(val color: Int, val timeOfDeath: Long = defaultTime()) {
    companion object {
        fun timeByDuration(duration: Float): Long {
            val nanoTime = Time.nanoTime
            return nanoTime + min((duration * 1e9f).toLongOr(), Long.MAX_VALUE - nanoTime)
        }

        fun defaultTime(): Long = Time.nanoTime + 5000 * MILLIS_TO_NANOS
    }
}