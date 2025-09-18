package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType.Companion.Float16xI
import me.anno.gpu.framebuffer.TargetType.Companion.Float32xI
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8xI
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureHelper
import me.anno.image.Image
import me.anno.maths.MinMax.max
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.Color.convertARGB2ABGR
import me.anno.utils.async.Callback
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT

/**
 * the easiest check whether an image has R and B channels inverted: if so, this will look correct
 * */
class BGRAImage(val src: Image) : Image(
    src.width, src.height, src.numChannels,
    src.hasAlphaChannel, src.offset, src.stride
) {

    override var width: Int
        get() = src.width
        set(value) {
            src.width = value
        }

    override var height: Int
        get() = src.height
        set(value) {
            src.height = value
        }

    override fun getIndex(x: Int, y: Int): Int {
        return src.getIndex(x, y)
    }

    override fun getRGB(index: Int): Int {
        // argb -> abgr
        return convertABGR2ARGB(src.getRGB(index))
    }

    override fun setRGB(index: Int, value: Int) {
        src.setRGB(index, convertARGB2ABGR(value))
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        if (src is GPUImage) {
            // if source has float precision, use that
            val tex = src.texture
            val useFP = when (TextureHelper.getNumberType(tex.internalFormat)) {
                GL_HALF_FLOAT -> Float16xI
                GL_FLOAT -> Float32xI
                else -> UInt8xI
            }
            val type = useFP[max(src.numChannels - 1, 0)]
            TextureMapper.mapTexture(src.texture, texture, "bgra", type, callback)
        } else super.createTextureImpl(texture, checkRedundancy, callback)
    }
}