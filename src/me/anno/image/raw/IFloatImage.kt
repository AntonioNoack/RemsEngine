package me.anno.image.raw

import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import kotlin.math.floor

abstract class IFloatImage(
    width: Int, height: Int, channels: Int,
    val map: ColorMap = LinearColorMap.default
) : Image(width, height, channels, channels > 3) {

    /**
     * interpolated value access
     * */
    open fun getValue(x: Float, y: Float, channel: Int): Float {
        val width = width
        val height = height
        return when {
            width == 1 && height == 1 -> getValue(0, channel)
            width == 1 -> {
                val y2 = clamp(y, 0f, height - 2f)
                val y0 = floor(y2)
                val fy = clamp(y2 - y0, 0f, 0f)
                val i0 = y0.toInt() * width
                val i1 = i0 + width
                mix(getValue(i0, channel), getValue(i1, channel), fy)
            }
            height == 1 -> {
                val x2 = clamp(x, 0f, width - 2f)
                val x0 = floor(x2)
                val fx = clamp(x2 - x0, 0f, 1f)
                val i0 = x0.toInt()
                mix(getValue(i0, channel), getValue(i0 + 1, channel), fx)
            }
            else -> {
                val x2 = clamp(x, 0f, width - 2f)
                val y2 = clamp(y, 0f, height - 2f)
                val x0 = floor(x2)
                val y0 = floor(y2)
                val fx = clamp(x2 - x0, 0f, 1f)
                val fy = clamp(y2 - y0, 0f, 0f)
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

    abstract fun normalize(): IFloatImage

    fun getColor(f: Float) = clamp(f * 255f, 0f, 255f).toInt()

    override fun getRGB(index: Int): Int {
        return when (numChannels) {
            1 -> map.getColor(getValue(index, 0))
            2 -> {
                getColor(getValue(index, 0)).shl(16) or
                        getColor(getValue(index, 1)).shl(8)
            }
            3 -> {
                getColor(getValue(index, 0)).shl(16) or
                        getColor(getValue(index, 1)).shl(8) or
                        getColor(getValue(index, 2))
            }
            else -> {
                getColor(getValue(index, 0)).shl(16) or
                        getColor(getValue(index, 1)).shl(8) or
                        getColor(getValue(index, 2)) or
                        getColor(getValue(index, 3)).shl(24)
            }
        }
    }

}