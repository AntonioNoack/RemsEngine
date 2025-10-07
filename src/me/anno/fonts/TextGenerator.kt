package me.anno.fonts

import me.anno.fonts.keys.FontKey
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.utils.async.Callback

interface TextGenerator {

    val fontKey: FontKey

    fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int
    fun generateTexture(
        text: CharSequence,
        widthLimit: Int, heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int = -1, // white with full alpha
        backgroundColor: Int = 255 shl 24 // white with no alpha
    )

    fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int = -1, // white with full alpha
        backgroundColor: Int = 255 shl 24 // white with no alpha
    )

    /**
     * distance from the top of generated textures to the lowest point of characters like A;
     * ~ 0.8 * fontSize, = ascent
     * */
    fun getBaselineY(): Float

    /**
     * distance from the top of generated textures to the bottom;
     * ~ [1.0,1.5] * fontSize, = ascent + descent
     * */
    fun getLineHeight(): Float

    companion object {
        const val TEXTURE_PADDING_W = 2
        const val TEXTURE_PADDING_H = 1
    }
}