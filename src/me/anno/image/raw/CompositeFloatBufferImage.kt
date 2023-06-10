package me.anno.image.raw

import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths.max
import java.nio.FloatBuffer

class CompositeFloatBufferImage(
    width: Int, height: Int,
    val channels: Array<FloatBuffer>,
    map: ColorMap = LinearColorMap.default
) : IFloatImage(width, height, channels.size, map) {

    /**
     * gets the value on the field
     * */
    override fun getValue(index: Int, channel: Int): Float {
        return channels[channel][index]
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    override fun setValue(index: Int, channel: Int, value: Float): Float {
        val channel2 = channels[channel]
        val previous = channel2[index]
        channel2.put(index, value)
        return previous
    }

    override fun getRGB(index: Int): Int {
        return when (numChannels) {
            1 -> map.getColor(channels[0][index])
            2 -> {
                getColor(channels[0][index]).shl(16) or getColor(channels[1][index]).shl(8)
            }
            3 -> {
                getColor(channels[0][index]).shl(16) or
                        getColor(channels[1][index]).shl(8) or
                        getColor(channels[2][index])
            }
            else -> {
                getColor(channels[0][index]).shl(16) or getColor(channels[1][index]).shl(8) or getColor(channels[2][index]) or
                        getColor(channels[3][index]).shl(24)
            }
        }
    }

    override fun normalize(): CompositeFloatBufferImage {
        var min = 0f
        var max = 0f
        for (channel in channels) {
            for (i in 0 until channel.capacity()) {
                val v = channel[i]
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        if (min < 0f || max > 0f) {
            val div = 1f / max(-min, max)
            for (channel in channels) {
                for (i in 0 until channel.capacity()) {
                    channel.put(i, channel[i] * div)
                }
            }
        }
        return this
    }

}