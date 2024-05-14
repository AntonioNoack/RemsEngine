package me.anno.image.raw

import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.smoothStep
import kotlin.math.floor

abstract class IFloatImage(
    width: Int, height: Int, channels: Int,
    val map: ColorMap, offset: Int, stride: Int
) : Image(width, height, channels, channels > 3, offset, stride) {

    constructor(width: Int, height: Int, channel: Int, map: ColorMap) :
            this(width, height, channel, map, 0, width)

    private var hm1f = height - 1f
    private var wm1f = width - 1f

    private var wm2f = width - 2f
    private var hm2f = height - 2f

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
            val dstImage = FloatImage(width, height, numChannels, map)
            for (c in 0 until numChannels) {
                for (i in 0 until width * height) {
                    dstImage.setValue(i, c, getValue(i, c))
                }
            }
            dstImage
        }
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): IFloatImage {
        return CroppedFloatImage(this, x0, y0, w0, h0)
    }

    open fun normalize(): IFloatImage {
        return toFloatImage().normalize()
    }

    open fun reinhard(): IFloatImage {
        return toFloatImage().reinhard()
    }

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