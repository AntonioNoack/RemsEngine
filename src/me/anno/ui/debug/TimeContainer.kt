package me.anno.ui.debug

import me.anno.maths.Maths.clamp
import kotlin.math.max

class TimeContainer(val width: Int, val color: Int): Comparable<TimeContainer> {
    var maxValue = 0f
    val values = FloatArray(width)
    var nextIndex = 0
    fun putValue(value: Float) {
        values[nextIndex] = value
        nextIndex = (nextIndex + 1) % width
        val max = values.maxOrNull()!!
        maxValue = max(maxValue * clamp((1f - 3f * value), 0f, 1f), max)
    }

    override fun compareTo(other: TimeContainer): Int {
        return maxValue.compareTo(other.maxValue)
    }
}