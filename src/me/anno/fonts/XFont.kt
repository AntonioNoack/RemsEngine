package me.anno.fonts

import me.anno.gpu.texture.Texture2D

interface XFont {
    fun generateTexture(text: String, fontSize: Float): Texture2D?
}