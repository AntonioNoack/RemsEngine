package me.anno.fonts.keys

import me.anno.fonts.Font
import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.fonts.FontManager.getFontSizeIndex
import me.anno.fonts.FontManager.limitHeight
import me.anno.fonts.FontManager.limitWidth
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.contentHashCode
import me.anno.utils.types.Strings.isBlank2

data class TextCacheKey(val text: CharSequence, val fontName: String, val properties: Int, val limits: Int) {

    constructor(text: CharSequence, fontName: String, properties: Int, widthLimit: Int, heightLimit: Int) :
            this(text, fontName, properties, GFXx2D.getSize(widthLimit, heightLimit))

    constructor(text: String, font: Font, widthLimit: Int, heightLimit: Int, grayscale: Boolean) :
            this(text, font.name, getProperties(font.sizeIndex, font, grayscale), widthLimit, heightLimit)

    constructor(text: String, font: Font) : this(text, font, GFX.maxTextureSize, GFX.maxTextureSize, false)

    fun fontSize(): Float = getAvgFontSize(fontSizeIndex())
    fun fontSizeIndex(): Int = properties.shr(3)
    fun isItalic(): Boolean = properties.hasFlag(4)
    fun isBold(): Boolean = properties.hasFlag(2)
    fun isGrayscale(): Boolean = properties.hasFlag(1)

    fun createFont() = Font(fontName, getAvgFontSize(fontSizeIndex()), isBold(), isItalic())

    private var _hashCode = generateHashCode()
    override fun hashCode() = _hashCode

    fun generateHashCode(): Int {
        var result = text.contentHashCode()
        result = 31 * result + fontName.hashCode()
        result = 31 * result + properties
        result = 31 * result + limits
        return result
    }

    val widthLimit get() = GFXx2D.getSizeX(limits)
    val heightLimit get() = GFXx2D.getSizeY(limits)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextCacheKey) return false
        return _hashCode == other._hashCode && text.contentEquals(other.text) && fontName == other.fontName &&
                properties == other.properties && limits == other.limits
    }

    override fun toString(): String = "$fontName, $properties, $widthLimit, $heightLimit, '$text'"

    companion object {
        fun getProperties(fontSizeIndex: Int, font: Font, grayscale: Boolean): Int {
            return getProperties(fontSizeIndex, font.isBold, font.isItalic, grayscale)
        }

        fun getProperties(fontSizeIndex: Int, isBold: Boolean, isItalic: Boolean, grayscale: Boolean): Int {
            return fontSizeIndex * 8 + isItalic.toInt(4) + isBold.toInt(2) + grayscale.toInt(1)
        }

        fun getTextCacheKey(
            font: Font, text: CharSequence,
            widthLimit: Int, heightLimit: Int,
            grayscale: Boolean
        ): TextCacheKey {
            val fontSizeIndex = font.sizeIndex
            val properties = getProperties(fontSizeIndex, font, grayscale)
            return getTextCacheKey(font, text, widthLimit, heightLimit, properties)
        }

        fun getTextCacheKey(
            font: Font, text: CharSequence,
            widthLimit: Int, heightLimit: Int,
            properties: Int
        ): TextCacheKey {

            val wl = limitWidth(font, widthLimit)
            val hl = limitHeight(font, text, wl, heightLimit)

            val fontName = font.name
            return TextCacheKey(text, fontName, properties, wl, hl)
        }

        fun getTextCacheKey(
            font: Font, text: String,
            widthLimit: Int, heightLimit: Int
        ): TextCacheKey? {

            if (text.isBlank2()) return null
            val fontSize = font.size

            val fontSizeIndex = getFontSizeIndex(fontSize)
            val properties = fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2)
            return getTextCacheKey(font, text, widthLimit, heightLimit, properties)
        }
    }
}