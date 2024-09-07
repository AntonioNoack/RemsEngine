package me.anno.engine.ui.render

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.callbacks.I2U
import kotlin.math.abs
import kotlin.math.max

class RenderSize {

    var renderWidth = 0
    var renderHeight = 0
    private var lastChangeTime = 0L

    fun isSame(width: Int, currWidth: Int): Boolean {
        return abs(width - currWidth) < 16 + max(width, currWidth).shr(1)
    }

    fun resize(width: Int, height: Int, time: Long) {
        renderWidth = width
        renderHeight = height
        lastChangeTime = time
    }

    fun render(width: Int, height: Int, callback: I2U) {
        updateSize(width, height)
        callback.call(width, height)
    }

    fun updateSize(width: Int, height: Int) {
        val time = Time.nanoTime
        if (isSame(width, renderWidth) && isSame(height, renderHeight)) {
            if (time - lastChangeTime > 500 * MILLIS_TO_NANOS) {
                resize(width, height, time)
            }
        } else {
            resize(width, height, time)
        }
    }
}