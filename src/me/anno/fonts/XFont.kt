package me.anno.fonts

import me.anno.gpu.texture.ITexture2D

interface XFont {
    fun generateTexture(text: String, fontSize: Float, widthLimit: Int): ITexture2D?
}