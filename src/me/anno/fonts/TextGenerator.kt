package me.anno.fonts

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.utils.async.Callback

interface TextGenerator {
    fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int
    fun generateTexture(
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int = -1,
        backgroundColor: Int = 255 shl 24,
        extraPadding: Int = 0
    )

    fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int = -1,
        backgroundColor: Int = 255 shl 24,
        extraPadding: Int = 0
    )
}