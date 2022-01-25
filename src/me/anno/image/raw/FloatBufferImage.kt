package me.anno.image.raw

import me.anno.image.Image
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import java.nio.FloatBuffer

class FloatBufferImage(
    width: Int, height: Int, channels: Int,
    val data: FloatBuffer,
    val map: ColorMap = LinearColorMap.default
) : Image(width, height, channels, channels > 3) {

    // todo interpolated access

    /**
     * gets the value on the field, clamps the coordinates
     * */
    fun getValue(x: Int, y: Int, channel: Int): Float {
        return getValue(getIndex(x, y), channel)
    }

    /**
     * gets the value on the field
     * */
    fun getValue(index: Int, channel: Int): Float {
        return data[index * numChannels + channel]
    }

    /**
     * sets the value in the buffer, and returns the previous value;
     * clamps the coordinates
     * */
    fun setValue(x: Int, y: Int, channel: Int, value: Float): Float {
        val i = getIndex(x, y) * numChannels + channel
        val previous = data[i]
        data.put(i, value)
        return previous
    }

    /**
     * sets the value in the buffer, and returns the previous value
     * */
    fun setValue(index: Int, channel: Int, value: Float): Float {
        val i = index * numChannels + channel
        val previous = data[i]
        data.put(i, value)
        return previous
    }

    fun getColor(f: Float) = clamp(f * 255f, 0f, 255f).toInt()

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