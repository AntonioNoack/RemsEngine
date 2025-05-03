package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.utils.Color
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.async.Callback
import me.anno.utils.pooling.JomlPools
import org.joml.Vector4f
import kotlin.math.max

class FloatImage(
    width: Int, height: Int, channels: Int,
    val data: FloatArray = FloatArray(width * height * channels),
    map: ColorMap, offset: Int, stride: Int
) : IFloatImage(width, height, channels, map, offset, stride) {

    constructor(width: Int, height: Int, channels: Int) :
            this(width, height, channels, LinearColorMap.default)

    constructor(width: Int, height: Int, channels: Int, data: FloatArray) : this(
        width, height, channels, data, LinearColorMap.default,
        0, channels * width
    )

    constructor(width: Int, height: Int, channels: Int, map: ColorMap) : this(
        width, height, channels, FloatArray(width * height * channels), map,
        0, channels * width
    )

    constructor(width: Int, height: Int, channels: Int, data: FloatArray, offset: Int, stride: Int) :
            this(width, height, channels, data, LinearColorMap.default, offset, stride)

    override fun getIndex(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return offset + xi * numChannels + yi * stride
    }

    /**
     * gets the value on the field
     * */
    override fun getValue(index: Int, channel: Int): Float {
        return data[index + channel]
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    override fun setValue(index: Int, channel: Int, value: Float): Float {
        val i = index + channel
        val previous = data[i]
        data[i] = value
        return previous
    }

    override fun setRGB(index: Int, value: Vector4f) {
        setValue(index, 0, value.x)
        if (numChannels > 1) setValue(index, 1, value.y)
        if (numChannels > 2) setValue(index, 2, value.z)
        if (numChannels > 3) setValue(index, 3, value.w)
    }

    override fun setRGB(index: Int, value: Int) {
        setValue(index, 0, value.r01())
        if (numChannels > 1) setValue(index, 1, value.g01())
        if (numChannels > 2) setValue(index, 2, value.b01())
        if (numChannels > 3) setValue(index, 3, value.a01())
    }

    override fun getRGB(index: Int): Int {
        val numChannels = numChannels
        return if (numChannels == 1) {
            map.getColor(data[index])
        } else {
            Color.rgba(
                getColor(data[index]),
                getColor(data[index + 1]),
                if (numChannels > 2) getColor(data[index + 2]) else 0,
                if (numChannels > 3) getColor(data[index + 3]) else 255
            )
        }
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        val tmp = FloatArray(width * height * numChannels)
        val lineLength = width * numChannels
        for (y in 0 until height) {
            val src0 = getIndex(0, height - 1 - y)
            val src1 = src0 + lineLength
            val dstI = y * lineLength
            data.copyInto(tmp, dstI, src0, src1)
        }
        texture.create(TargetType.Float32xI[numChannels - 1], tmp)
        callback.ok(texture)
    }

    fun normalized() = clone().normalize()

    fun clone(): FloatImage {
        return FloatImage(width, height, numChannels, data.copyOf(), map, offset, stride)
    }

    override fun normalize(): FloatImage {
        var min = 0f
        var max = 0f
        for (v in data) {
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        if (min < 0f || max > 0f) {
            mul(1f / Maths.max(-min, max))
        }
        return this
    }

    override fun reinhard(): IFloatImage {
        for (i in data.indices) {
            val ci = max(data[i], 0f)
            data[i] = ci / (1f + ci)
        }
        return this
    }

    fun normalize01(): FloatImage {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (v in data) {
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        if (min < max) {
            val scale = 1f / max(max, -min)
            for (i in data.indices) {
                data[i] *= scale
            }
        }
        return this
    }

    fun mul(s: Float): FloatImage {
        if (s != 1f) {
            for (i in data.indices) {
                data[i] *= s
            }
        }
        return this
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return FloatImage(w0, h0, numChannels, data, map, getIndex(x0, y0), stride)
    }
}