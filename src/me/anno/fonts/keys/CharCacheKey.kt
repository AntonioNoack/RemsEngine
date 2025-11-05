package me.anno.fonts.keys

import me.anno.fonts.Font

data class CharCacheKey(
    val font: Font, val codepoint: Int,
    val grayscale: Boolean
)