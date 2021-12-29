package me.anno.fonts.keys

import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.ui.base.Font
import me.anno.utils.types.Booleans.toInt

class TextCacheKey(
    val text: CharSequence,
    val fontName: String,
    val properties: Int,
    val widthLimit: Int,
    val heightLimit: Int
) {

    constructor(
        text: String, fontName: String, fontSizeIndex: Int, isBold: Boolean, isItalic: Boolean,
        widthLimit: Int, heightLimit: Int
    ) : this(text, fontName, getProperties(fontSizeIndex, isBold, isItalic), widthLimit, heightLimit)

    constructor(text: String, font: Font, widthLimit: Int, heightLimit: Int) :
            this(text, font.name, getProperties(font.sizeIndex, font), widthLimit, heightLimit)

    fun fontSizeIndex() = properties.shr(3)
    fun isItalic() = properties.and(4) != 0
    fun isBold() = properties.and(2) != 0

    // fun isSize() = properties.and(1) != 0
    fun createFont() = Font(fontName, getAvgFontSize(fontSizeIndex()), isBold(), isItalic())

    var hashCode = generateHashCode()
    override fun hashCode() = hashCode

    fun updateHashCode() {
        hashCode = generateHashCode()
    }

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

    override fun toString(): String = "$fontName, $properties, $widthLimit, $heightLimit, '$text'"

    companion object {
        fun getProperties(fontSizeIndex: Int, font: Font): Int {
            return getProperties(fontSizeIndex, font.isBold, font.isItalic)
        }

        fun getProperties(fontSizeIndex: Int, isBold: Boolean, isItalic: Boolean): Int {
            return fontSizeIndex * 8 + isItalic.toInt(4) + isBold.toInt(2)
        }
    }

}