package me.anno.fonts.keys

import me.anno.fonts.Font

data class TextCacheKey(
    val font: Font, val text: CharSequence,
    val widthLimit: Int, val heightLimit: Int,
    val grayscale: Boolean
)