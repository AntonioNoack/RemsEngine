package me.anno.image.raw

import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths.min
import java.nio.FloatBuffer

class FloatBufferImage(
    width: Int, height: Int, channels: Int,
    val data: FloatBuffer,
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
        data.put(i, value)
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

    fun toFloatImage(data: FloatArray = FloatArray(width * height * numChannels)): FloatImage {
        val ownData = this.data
        for (i in 0 until min(ownData.limit(), data.size)) {
            data[i] = ownData[i]
        }
        return FloatImage(width, height, numChannels, data, map)
    }

}