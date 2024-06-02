package me.anno.ui.debug

import me.anno.Time
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.mix
import me.anno.ui.Style
import me.anno.ui.utils.FunctionPanel

/**
 * function panel
 * add values as time passes
 * */
open class TrackingPanel(val getValues: List<() -> Double>, val colors: IntArray, style: Style) : FunctionPanel(style) {

    override fun getNumFunctions(): Int = numChannels
    override fun getValue(index: Int, x: Double): Double {
        val relativeTime = (x * SECONDS_TO_NANOS).toLong() + times[0]
        var timeIndex = times.binarySearch(relativeTime)
        if (timeIndex < 0) {
            if (timeIndex == -1) return Double.NaN // before recording
            // interpolate
            timeIndex = -1 - timeIndex
            if (timeIndex + 1 >= size) return Double.NaN // in the future
            val i0 = timeIndex * numChannels + index
            val t0 = times[timeIndex - 1]
            val t1 = times[timeIndex]
            return mix(
                values[i0 - numChannels],
                values[i0],
                (relativeTime - t0).toDouble() / (t1 - t0)
            )
        } else {
            // get value directly
            return values[timeIndex * numChannels + index]
        }
    }

    override fun getColor(index: Int): Int {
        return colors[index]
    }

    // x,y,z,time
    val numChannels = getValues.size
    var values = DoubleArray(numChannels * 64)
    var times = LongArray(64)
    var size = 0

    var minValue = Double.POSITIVE_INFINITY
    var maxValue = Double.NEGATIVE_INFINITY

    init {
        times.fill(Long.MAX_VALUE)
        // fill remaining time with infinity
        // record start time & values
        record()
    }

    fun record() {
        if (size >= times.size) {
            val newSize = times.size * 2
            times = times.copyOf(newSize)
            times.fill(Long.MAX_VALUE, size, newSize)
            values = values.copyOf(newSize * numChannels)
        }
        val index = size++
        val time = Time.nanoTime
        times[index] = time
        val i0 = index * numChannels
        for (i in getValues.indices) {
            val vi = getValues[i]()
            values[i0 + i] = vi
            // don't save NaN!
            if (vi < minValue) minValue = vi
            if (vi > maxValue) maxValue = vi
        }
    }

    // todo scale x and y separately
    // todo draw x axis with time units

    fun autoScale() {
        if (maxValue > minValue) {
            scale.set(0.85 * height / (maxValue - minValue))
            targetScale.set(scale)
        }
    }

    fun autoMove() {
        // todo only move if there isn't enough space?
        center.set(
            (Time.nanoTime - times[0]) / 1e9 - 0.2 * height / scale.y,
            -(minValue + maxValue) * 0.5,
        )
        target.set(center)
    }

    override fun onUpdate() {
        super.onUpdate()
        record()
        // move, if not using this panel
        if (!isAnyChildInFocus) {
            autoScale()
            autoMove()
        }
        invalidateDrawing() // we got new values ^^
    }
}