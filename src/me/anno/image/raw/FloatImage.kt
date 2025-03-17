package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths
import me.anno.utils.Color
import me.anno.utils.async.Callback
import kotlin.math.max

class FloatImage(
    width: Int, height: Int, channels: Int,
    val data: FloatArray = FloatArray(width * height * channels),
    map: ColorMap = LinearColorMap.default, offset: Int = 0, stride: Int = width
) : IFloatImage(width, height, channels, map, offset, stride) {

    constructor(width: Int, height: Int, channels: Int, map: ColorMap) :
            this(width, height, channels, FloatArray(width * height * channels), map)

    /**
     * gets the value on the field
     * */
    override fun getValue(index: Int, channel: Int): Float {
        return data[index * numChannels + channel]
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    override fun setValue(index: Int, channel: Int, value: Float): Float {
        val i = index * numChannels + channel
        val previous = data[i]
        data[i] = value
        return previous
    }

    override fun getRGB(index: Int): Int {
        val nc = numChannels
        return if (nc == 1) {
            map.getColor(data[index])
        } else {
            val idx = index * nc
            Color.rgba(
                getColor(data[idx]),
                getColor(data[idx + 1]),
                if (nc > 2) getColor(data[idx + 2]) else 0,
                if (nc > 3) getColor(data[idx + 3]) else 255
            )
        }
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        val tmp = FloatArray(width * height)
        for (y in 0 until height) {
            val src0 = getIndex(0, height - 1 - y)
            val src1 = src0 + width
            data.copyInto(tmp, y * width, src0, src1)
        }
        texture.create(TargetType.Float32xI[numChannels - 1], tmp)
        callback.ok(texture)
    }

    fun normalized() = clone().normalize()

    fun clone(): FloatImage {
        return FloatImage(width, height, numChannels, data.copyOf(), map)
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
            val scale = 1f / (max - min)
            for (i in data.indices) {
                data[i] = (data[i] - min) * scale
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