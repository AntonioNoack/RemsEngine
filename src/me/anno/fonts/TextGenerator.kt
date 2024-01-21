package me.anno.fonts

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray

interface TextGenerator {
    fun calculateSize(text: CharSequence, fontSize: Float, widthLimit: Int, heightLimit: Int): Int
    fun generateTexture(
        text: CharSequence,
        fontSize: Float,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        textColor: Int = -1,
        backgroundColor: Int = 255 shl 24,
        extraPadding: Int = 0
    ): ITexture2D?
    fun generateASCIITexture(
        portableImages: Boolean,
        textColor: Int = -1,
        backgroundColor: Int = 255 shl 24,
        extraPadding: Int = 0
    ): Texture2DArray
}