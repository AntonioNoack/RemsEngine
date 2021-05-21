package me.anno.ui.debug

import me.anno.utils.Maths.clamp
import kotlin.math.max

class TimeContainer(val width: Int, val color: Int) {
    var maxValue = 0f
    val values = FloatArray(width)
    var nextIndex = 0
    fun putValue(value: Float) {
        values[nextIndex] = value
        nextIndex = (nextIndex + 1) % width
        val max = values.maxOrNull()!!
        maxValue = max(maxValue * clamp((1f - 3f * value), 0f, 1f), max)
    }
}