package me.anno.image.raw

import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

@Suppress("unused")
class CompositeFloatImage(
    width: Int, height: Int,
    val channels: Array<FloatArray>,
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
        val channel1 = channels[channel]
        val previous = channel1[index]
        channel1[index] = value
        return previous
    }

}