package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

open class IntImage(
    width: Int, height: Int,
    val data: IntArray = IntArray(width * height),
    hasAlphaChannel: Boolean
) : Image(width, height, if (hasAlphaChannel) 4 else 3, hasAlphaChannel) {

    constructor(width: Int, height: Int, hasAlphaChannel: Boolean) :
            this(width, height, IntArray(width * height), hasAlphaChannel)

    fun setRGB(x: Int, y: Int, rgb: Int) {
        data[x + y * width] = rgb
    }

    fun setRGBSafely(x: Int, y: Int, rgb: Int) {
        if (x in 0 until width && y in 0 until height) {
            data[x + y * width] = rgb
        }
    }

    override fun getRGB(index: Int): Int = data[index]

    override fun createBufferedImage(): BufferedImage {
        val width = width
        val height = height
        val image = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        val dataBuffer = image.raster.dataBuffer as DataBufferInt
        val dataDst = dataBuffer.data
        val dataSrc = data
        // src, dst
        System.arraycopy(dataSrc, 0, dataDst, 0, dataSrc.size)
        return image
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        // data cloning is required, because the function in Texture2D switches the red and blue channels
        if (sync && GFX.isGFXThread()) {
            if (hasAlphaChannel) texture.createBGRA(cloneData(), checkRedundancy)
            else texture.createBGR(cloneData(), checkRedundancy)
        } else {
            val data1 = Texture2D.bufferPool[data.size * 4, false, false]
            val dataI = data1.asIntBuffer()
            dataI.put(this.data).position(0)
            if (checkRedundancy) texture.checkRedundancy(dataI)
            Texture2D.switchRGB2BGR(dataI)
            // for testing, convert the data into a byte buffer
            // -> 33% faster, partially because of wrong alignment and using 25% less data effectively
            texture.createTiled(
                if (hasAlphaChannel) TargetType.UByteTarget4 else TargetType.UByteTarget3,
                TargetType.UByteTarget4,
                dataI, data1
            )
        }
    }

    fun cloneData(): IntArray {
        val clone = Texture2D.intArrayPool[data.size, false, true]
        System.arraycopy(data, 0, clone, 0, data.size)
        return clone
    }

}