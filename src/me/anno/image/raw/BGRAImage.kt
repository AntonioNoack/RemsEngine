package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType.Companion.UByteTargets
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.convertABGR2ARGB

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
            // todo if source has float precision, use that
            val type = UByteTargets[base.numChannels - 1]
            TextureMapper.mapTexture(base.texture, texture, "bgra", type, callback)

        } else super.createTexture(texture, sync, checkRedundancy, callback)
    }
}