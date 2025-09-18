package me.anno.image.raw

import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.smoothStep
import me.anno.utils.Color
import kotlin.math.floor

abstract class IFloatImage(
    width: Int, height: Int, numChannels: Int,
    val map: ColorMap, offset: Int, stride: Int
) : Image(width, height, numChannels, numChannels > 3, offset, stride) {

    private val hm1f get() = height - 1f
    private val wm1f get() = width - 1f

    private val wm2f get() = width - 2f
    private val hm2f get() = height - 2f

    /**
     * interpolated value access
     * */
    open fun getValue(x: Float, y: Float, channel: Int = 0): Float {
        val width = width
        val height = height
        return when {
            width == 1 && height == 1 -> getValue(0, channel)
            width == 1 -> {
                val y2 = clamp(y, 0f, hm1f)
                val y0 = min(floor(y2), hm2f)
                val fy = clamp(y2 - y0, 0f, 1f)
                val i0 = y0.toInt() * width
                val i1 = i0 + width
                mix(getValue(i0, channel), getValue(i1, channel), fy)
            }
            height == 1 -> {
                val x2 = clamp(x, 0f, wm1f)
                val x0 = min(floor(x2), wm2f)
                val fx = clamp(x2 - x0, 0f, 1f)
                val i0 = x0.toInt()
                mix(getValue(i0, channel), getValue(i0 + 1, channel), fx)
            }
            else -> {
                val x2 = clamp(x, 0f, wm1f)
                val y2 = clamp(y, 0f, hm1f)
                val x0 = min(floor(x2), wm2f)
                val y0 = min(floor(y2), hm2f)
                val fx = clamp(x2 - x0, 0f, 1f)
                val fy = clamp(y2 - y0, 0f, 1f)
                val i0 = x0.toInt() + y0.toInt() * width
                val i1 = i0 + width
                mix(
                    mix(getValue(i0, channel), getValue(i0 + 1, channel), fx),
                    mix(getValue(i1, channel), getValue(i1 + 1, channel), fx),
                    fy
                )
            }
        }
    }

    @Suppress("unused")
    open fun getValueSmooth(x: Float, y: Float, channel: Int = 0): Float {
        val width = width
        val height = height
        return when {
            width == 1 && height == 1 -> getValue(0, channel)
            width == 1 -> {
                val y2 = clamp(y, 0f, hm1f)
                val y0 = min(floor(y2), hm2f)
                val fy = y2 - y0
                val i0 = y0.toInt() * width
                val i1 = i0 + width
                smoothStep(getValue(i0, channel), getValue(i1, channel), fy)
            }
            height == 1 -> {
                val x2 = clamp(x, 0f, wm1f)
                val x0 = min(floor(x2), wm2f)
                val fx = x2 - x0
                val i0 = x0.toInt()
                smoothStep(getValue(i0, channel), getValue(i0 + 1, channel), fx)
            }
            else -> {
                val x2 = clamp(x, 0f, wm1f)
                val y2 = clamp(y, 0f, hm1f)
                val x0 = min(floor(x2), wm2f)
                val y0 = min(floor(y2), hm2f)
                val fx = smoothStep(x2 - x0)
                val fy = smoothStep(y2 - y0)
                val i0 = x0.toInt() + y0.toInt() * width
                val i1 = i0 + width
                mix(
                    mix(getValue(i0, channel), getValue(i0 + 1, channel), fx),
                    mix(getValue(i1, channel), getValue(i1 + 1, channel), fx),
                    fy
                )
            }
        }
    }

    /**
     * gets the value on the field, clamps the coordinates
     * */
    open fun getValue(x: Int, y: Int, channel: Int): Float {
        return getValue(getIndex(x, y), channel)
    }

    /**
     * gets the value on the field
     * */
    abstract fun getValue(index: Int, channel: Int): Float

    /**
     * sets the value in the buffer, and returns the previous value;
     * clamps the coordinates
     * */
    open fun setValue(x: Int, y: Int, channel: Int, value: Float): Float {
        return setValue(getIndex(x, y), channel, value)
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    abstract fun setValue(index: Int, channel: Int, value: Float): Float

    fun toFloatImage(mustCopy: Boolean = false): FloatImage {
        return if (this is FloatImage) {
            if (mustCopy) clone() else this
        } else {
            val dstImage = FloatImage(width, height, this@IFloatImage.numChannels, map)
            for (c in 0 until this@IFloatImage.numChannels) {
                for (i in 0 until width * height) {
                    dstImage.setValue(i, c, getValue(i, c))
                }
            }
            dstImage
        }
    }

    open fun normalize(): IFloatImage {
        return toFloatImage().normalize()
    }

    open fun reinhard(): IFloatImage {
        return toFloatImage().reinhard()
    }

    fun getColor(f: Float): Int = clamp(f * 255f, 0f, 255f).toInt()

    override fun getRGB(index: Int): Int {
        val nc = this@IFloatImage.numChannels
        return if (nc == 1) {
            map.getColor(getValue(index, 0))
        } else {
            Color.rgba(
                getColor(getValue(index, 0)),
                getColor(getValue(index, 1)),
                if (nc > 2) getColor(getValue(index, 2)) else 0,
                if (nc > 3) getColor(getValue(index, 3)) else 255
            )
        }
    }
}