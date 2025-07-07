package me.anno.fonts.keys

import me.anno.fonts.Font
import me.anno.fonts.FontManager

data class FontKey(val name: String, val sizeIndex: Int, val bold: Boolean, val italic: Boolean) {
    val fontSize: Float = FontManager.getAvgFontSize(sizeIndex)
    fun toFont(): Font = Font(name, fontSize, bold, italic)
}