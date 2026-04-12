package me.anno.image.raw

import me.anno.gpu.GFX.supportsF16Targets
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.utils.Color
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.async.Callback
import me.anno.utils.types.Floats.float16ToFloat32
import me.anno.utils.types.Floats.float32ToFloat16
import org.joml.Vector4f
import kotlin.math.max

/**
 * stores float16 values instead of float32
 *
 * channel layout:
 * r = 0, g = 1, b = 2, a = 3
 * */
class HalfImage(
    width: Int, height: Int, channels: Int,
    val data: ShortArray = ShortArray(width * height * channels),
    map: ColorMap, offset: Int, stride: Int
) : IFloatImage(width, height, channels, map, offset, stride) {

    constructor(width: Int, height: Int, channels: Int) :
            this(width, height, channels, LinearColorMap.default)

    constructor(width: Int, height: Int, channels: Int, data: ShortArray) : this(
        width, height, channels, data, LinearColorMap.default,
        0, width
    )

    constructor(width: Int, height: Int, channels: Int, map: ColorMap) : this(
        width, height, channels, ShortArray(width * height * channels), map,
        0, width
    )

    constructor(width: Int, height: Int, channels: Int, data: ShortArray, offset: Int, stride: Int) :
            this(width, height, channels, data, LinearColorMap.default, offset, stride)

    private fun getRaw(index: Int): Float = float16ToFloat32(data[index].toInt())

    private fun setRaw(index: Int, v: Float) {
        data[index] = float32ToFloat16(v).toShort()
    }

    /**
     * gets the value on the field
     * */
    override fun getValue(index: Int, channel: Int): Float {
        return getRaw(index * numChannels + channel)
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    override fun setValue(index: Int, channel: Int, value: Float): Float {
        val i = index + channel
        val previous = getRaw(i)
        setRaw(i, value)
        return previous
    }

    override fun setRGB(index: Int, value: Vector4f) {
        val index = index * numChannels
        setValue(index, 0, value.x)
        if (numChannels > 1) setValue(index, 1, value.y)
        if (numChannels > 2) setValue(index, 2, value.z)
        if (numChannels > 3) setValue(index, 3, value.w)
    }

    override fun setRGB(index: Int, value: Int) {
        val index = index * numChannels
        setValue(index, 0, value.r01())
        if (numChannels > 1) setValue(index, 1, value.g01())
        if (numChannels > 2) setValue(index, 2, value.b01())
        if (numChannels > 3) setValue(index, 3, value.a01())
    }

    override fun getRGB(index: Int): Int {
        val index = index * numChannels
        val numChannels = numChannels
        return if (numChannels == 1) {
            map.getColor(getRaw(index))
        } else {
            Color.rgba(
                getColor(getRaw(index)),
                getColor(getRaw(index + 1)),
                if (numChannels > 2) getColor(getRaw(index + 2)) else 0,
                if (numChannels > 3) getColor(getRaw(index + 3)) else 255
            )
        }
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        val lineLength = width * numChannels
        if (supportsF16Targets) {
            val tmp = ShortArray(height * lineLength)
            repeat(height) { y ->
                val src0 = getIndex(0, height - 1 - y) * numChannels
                val src1 = src0 + lineLength
                val dstI = y * lineLength
                data.copyInto(tmp, dstI, src0, src1)
            }
            texture.create(TargetType.Float16xI[numChannels - 1], tmp)
        } else {
            val tmp = FloatArray(height * lineLength)
            repeat(height) { y ->
                val src0 = getIndex(0, height - 1 - y) * numChannels
                val src1 = src0 + lineLength
                val dstI = y * lineLength
                copyIntoX(tmp, dstI, src0, src1)
            }
            texture.create(TargetType.Float32xI[numChannels - 1], tmp)
        }
        callback.ok(texture)
    }

    private fun copyIntoX(dst: FloatArray, dstI: Int, src0: Int, src1: Int) {
        val data = data
        for (i in src0 until src1) {
            dst[dstI + i] = float16ToFloat32(data[i].toInt())
        }
    }

    fun normalized() = clone().normalize()

    fun clone(): HalfImage {
        return HalfImage(width, height, numChannels, data.copyOf(), map, offset, stride)
    }

    override fun normalize(): HalfImage {
        var min = 0f
        var max = 0f
        for (vs in data) {
            val v = float16ToFloat32(vs.toInt())
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        val scale = 1f / max(-min, max)
        if (scale != 1f && scale.isFinite()) mul(scale)
        return this
    }

    override fun reinhard(): IFloatImage {
        for (i in data.indices) {
            val ci = max(float16ToFloat32(data[i].toInt()), 0f)
            data[i] = float32ToFloat16(ci / (1f + ci)).toShort()
        }
        return this
    }

    fun mul(scale: Float) {
        if (scale == 1f) return
        for (i in data.indices) {
            data[i] = float32ToFloat16(float16ToFloat32(data[i].toInt()) * scale).toShort()
        }
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return HalfImage(w0, h0, numChannels, data, map, getIndex(x0, y0), stride)
    }
}