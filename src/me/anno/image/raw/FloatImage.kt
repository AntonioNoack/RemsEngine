package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths
import me.anno.utils.Color.black
import me.anno.utils.structures.Callback
import kotlin.math.max

class FloatImage(
    width: Int, height: Int, channels: Int,
    val data: FloatArray = FloatArray(width * height * channels),
    map: ColorMap = LinearColorMap.default
) : IFloatImage(width, height, channels, map) {

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
        return when (numChannels) {
            1 -> map.getColor(data[index])
            2 -> {
                val idx = index * 2
                getColor(data[idx]).shl(16) or
                        getColor(data[idx + 1]).shl(8) or black
            }
            3 -> {
                val idx = index * 3
                getColor(data[idx]).shl(16) or
                        getColor(data[idx + 1]).shl(8) or
                        getColor(data[idx + 2]) or black
            }
            else -> {
                val idx = index * numChannels
                getColor(data[idx]).shl(16) or
                        getColor(data[idx + 1]).shl(8) or
                        getColor(data[idx + 2]) or
                        getColor(data[idx + 3]).shl(24)
            }
        }
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (sync) {
            texture.create(TargetType.Float32xI[numChannels - 1], data)
            callback.ok(texture)
        } else {
            GFX.addGPUTask("CompFBI.cTex", width, height) {
                texture.create(TargetType.Float32xI[numChannels - 1], data)
                callback.ok(texture)
            }
        }
    }

    fun normalized() = clone().normalize()

    fun clone(): FloatImage {
        return FloatImage(width, height, numChannels, data.copyOf())
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

    fun abs(): IFloatImage {
        for (i in data.indices) {
            data[i] = kotlin.math.abs(data[i])
        }
        return this
    }
}