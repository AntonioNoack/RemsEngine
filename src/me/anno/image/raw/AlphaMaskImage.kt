package me.anno.image.raw

import me.anno.utils.async.Callback
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureHelper
import me.anno.image.Image
import me.anno.utils.Color.hex24
import org.lwjgl.opengl.GL46C

/**
 * maps a component like R/G/B/A onto 000V or 111V (black or white mask)
 * */
class AlphaMaskImage(val src: Image, val inverse: Boolean, val channel: Char, color: Int) :
    Image(src.width, src.height, 1, false) {

    private val color = color and 0xffffff
    private val shift = "bgra".indexOf(channel) * 8

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (src is GPUImage && (color == 0 || color == 0xffffff)) {
            val map = if (inverse) channel.uppercaseChar() else channel
            val base = if (color == 0) "000" else "111"
            val tex = src.texture
            val type = when (TextureHelper.getNumberType(tex.internalFormat)) {
                GL46C.GL_FLOAT -> TargetType.Float32x4
                GL46C.GL_HALF_FLOAT -> TargetType.Float16x4
                else -> TargetType.UInt8x4
            }
            TextureMapper.mapTexture(
                src.texture, texture, "$base$map",
                type, callback
            )
        } else super.createTexture(texture, sync, checkRedundancy, callback)
    }

    private fun getValue(index: Int): Int {
        val base = src.getRGB(index).ushr(shift).and(255)
        return if (inverse) 255 - base else base
    }

    override fun getRGB(index: Int): Int {
        return getValue(index).shl(24) or color
    }

    override fun toString(): String {
        return "AlphaMaskImage { $src, ${if (inverse) "1-" else ""}$channel, #${hex24(color)} }"
    }
}