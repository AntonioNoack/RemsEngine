package me.anno.image.raw

import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FloatImage(
    width: Int, height: Int, channels: Int,
    val data: FloatArray,
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
                getColor(data[idx]).shl(16) or getColor(data[idx + 1]).shl(8)
            }
            3 -> {
                val idx = index * 3
                getColor(data[idx]).shl(16) or getColor(data[idx + 1]).shl(8) or getColor(data[idx + 2])
            }
            else -> {
                val idx = index * numChannels
                getColor(data[idx]).shl(16) or getColor(data[idx + 1]).shl(8) or getColor(data[idx + 2]) or
                        getColor(data[idx + 3]).shl(24)
            }
        }
    }

    fun toFloatBufferImage(
        data: FloatBuffer = ByteBuffer.allocateDirect(width * height * numChannels * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    ): FloatBufferImage {
        val ownData = this.data
        for (i in 0 until Maths.min(ownData.size, data.limit())) {
            data.put(i, ownData[i])
        }
        return FloatBufferImage(width, height, numChannels, data, map)
    }

    override fun normalize(): IFloatImage {
        var min = 0f
        var max = 0f
        for (v in data) {
            if (v < min) min = v
            if (v > max) max = v
        }
        if (min < 0f || max > 0f) {
            val div = 1f / Maths.max(-min, max)
            for (i in data.indices) {
                data[i] *= div
            }
        }
        return this
    }

}