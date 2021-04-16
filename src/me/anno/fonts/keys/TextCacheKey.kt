package me.anno.fonts.keys

import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.ui.base.Font
import me.anno.utils.types.Booleans.toInt

data class TextCacheKey(
    val text: String,
    val fontName: String,
    val properties: Int,
    val widthLimit: Int
) {

    fun fontSizeIndex() = properties.shr(3)
    fun isItalic() = properties.and(4) != 0
    fun isBold() = properties.and(2) != 0
    fun isSize() = properties.and(1) != 0
    fun createFont() = Font(fontName, getAvgFontSize(fontSizeIndex()), isBold(), isItalic())

    val hashCode = generateHashCode()
    override fun hashCode() = hashCode

    fun generateHashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + fontName.hashCode()
        result = 31 * result + properties
        result = 31 * result + widthLimit
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextCacheKey

        if (hashCode != other.hashCode) return false
        if (text != other.text) return false
        if (fontName != other.fontName) return false
        if (properties != other.properties) return false
        if (widthLimit != other.widthLimit) return false

        return true
    }


    companion object {
        fun getProperties(fontSizeIndex: Int, font: Font, isSize: Boolean): Int {
            return fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2) + isSize.toInt(1)
        }
    }

}