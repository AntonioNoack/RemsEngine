package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.Color.black
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

    fun toFloatImage(data: FloatArray = FloatArray(width * height * numChannels)): FloatImage {
        val ownData = this.data
        for (i in 0 until min(ownData.limit(), data.size)) {
            data[i] = ownData[i]
        }
        return FloatImage(width, height, numChannels, data, map)
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        if (sync) {
            texture.create(TargetType.FloatTargets[numChannels - 1], data)
            callback(texture, null)
        } else {
            GFX.addGPUTask("CompFBI.cTex", width, height) {
                texture.create(TargetType.FloatTargets[numChannels - 1], data)
                callback(texture, null)
            }
        }
    }

    override fun normalize(): FloatBufferImage {
        var min = 0f
        var max = 0f
        for (i in 0 until data.capacity()) {
            val v = data[i]
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        if (min < 0f || max > 0f) {
            val div = 1f / max(-min, max)
            for (i in 0 until data.capacity()) {
                data.put(i, data[i] * div)
            }
        }
        return this
    }

    override fun reinhard(): IFloatImage {
        for (i in 0 until data.capacity()) {
            val ci = max(data[i], 0f)
            data.put(i, ci / (1f + ci))
        }
        return this
    }
}