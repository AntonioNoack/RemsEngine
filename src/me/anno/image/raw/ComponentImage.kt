package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType.Companion.Float16x4
import me.anno.gpu.framebuffer.TargetType.Companion.Float32x4
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8x4
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureHelper
import me.anno.image.Image
import me.anno.utils.Color.black
import me.anno.utils.async.Callback
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT

/**
 * maps a component like R/G/B/A onto XXX1 (opaque, grayscale)
 * */
class ComponentImage(val src: Image, val inverse: Boolean, val channel: Char) :
    Image(src.width, src.height, 1, false) {

    private val shift = "bgra".indexOf(channel) * 8

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        if (src is GPUImage) {
            val map = if (inverse) channel.uppercaseChar() else channel
            val tex = src.texture
            val type = if (tex is Texture2D) {
                when (TextureHelper.getNumberType(tex.internalFormat)) {
                    GL_FLOAT -> Float32x4
                    GL_HALF_FLOAT -> Float16x4
                    else -> UInt8x4
                }
            } else UInt8x4
            TextureMapper.mapTexture(src.texture, texture, "$map$map${map}1", type, callback)
        } else super.createTextureImpl(texture, checkRedundancy, callback)
    }

    private fun getValue(index: Int): Int {
        val base = src.getRGB(index).ushr(shift).and(255)
        return if (inverse) 255 - base else base
    }

    override fun getRGB(index: Int): Int {
        return (getValue(index) * 0x10101) or black
    }

    override fun asIntImage(): IntImage {
        val newImage0 = src.asIntImage()
        val newData = newImage0.cloneData() // just to be safe to not modify the original
        val newImage1 = IntImage(newImage0.width, newImage0.height, newData, false)
        val size = newImage0.width * newImage0.height
        for (i in 0 until size) {
            newData[i] = newData[i].ushr(shift).and(255)
        }
        if (inverse) {
            for (i in 0 until size) {
                newData[i] = 255 - newData[i]
            }
        }
        for (i in 0 until size) {
            newData[i] = (newData[i] * 0x10101) or black
        }
        return newImage1
    }

    override fun toString(): String {
        return "ComponentImage { $src, ${if (inverse) "1-" else ""}$channel }"
    }
}