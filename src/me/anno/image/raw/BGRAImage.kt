package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType.Companion.Float16xI
import me.anno.gpu.framebuffer.TargetType.Companion.Float32xI
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8xI
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.maths.Maths.max
import me.anno.utils.Color.convertABGR2ARGB
import org.lwjgl.opengl.GL11C.GL_FLOAT
import org.lwjgl.opengl.GL30C.GL_HALF_FLOAT

/**
 * the easiest check whether an image has R and B channels inverted: if so, this will look correct
 * */
class BGRAImage(val base: Image) :
    Image(base.width, base.height, base.numChannels, base.hasAlphaChannel) {

    override var width: Int
        get() = base.width
        set(value) {
            base.width = value
        }

    override var height: Int
        get() = base.height
        set(value) {
            base.height = value
        }

    override fun getRGB(index: Int): Int {
        // argb -> abgr
        return convertABGR2ARGB(base.getRGB(index))
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        if (base is GPUImage) {
            // if source has float precision, use that
            val tex = base.texture
            val useFP = if (tex is Texture2D) {
                when (Texture2D.getNumberType(tex.internalFormat)) {
                    GL_HALF_FLOAT -> Float16xI
                    GL_FLOAT -> Float32xI
                    else -> UInt8xI
                }
            } else UInt8xI
            val type = useFP[max(base.numChannels - 1, 0)]
            TextureMapper.mapTexture(base.texture, texture, "bgra", type, callback)
        } else super.createTexture(texture, sync, checkRedundancy, callback)
    }
}