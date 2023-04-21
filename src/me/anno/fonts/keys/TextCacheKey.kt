package me.anno.fonts.keys

import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D
import me.anno.ui.base.Font
import me.anno.utils.types.Booleans.toInt

data class TextCacheKey(
    val text: CharSequence,
    val fontName: String,
    val properties: Int,
    val limits: Int
) {

    constructor(text: CharSequence, fontName: String, properties: Int, widthLimit: Int, heightLimit: Int) :
            this(text, fontName, properties, GFXx2D.getSize(widthLimit, heightLimit)) {
        if (widthLimit < 0 || heightLimit < 0)
            throw IllegalStateException()
    }

    constructor(text: String, font: Font, widthLimit: Int, heightLimit: Int) :
            this(text, font.name, getProperties(font.sizeIndex, font), widthLimit, heightLimit)

    constructor(text: String, font: Font) : this(text, font, GFX.maxTextureSize, GFX.maxTextureSize)

    fun fontSize(): Float = getAvgFontSize(fontSizeIndex())
    fun fontSizeIndex(): Int = properties.shr(3)
    fun isItalic(): Boolean = properties.and(4) != 0
    fun isBold(): Boolean = properties.and(2) != 0

    // fun isSize() = properties.and(1) != 0
    fun createFont() = Font(fontName, getAvgFontSize(fontSizeIndex()), isBold(), isItalic())

    var hashCode = generateHashCode()
    override fun hashCode() = hashCode

    @Suppress("unused")
    fun updateHashCode() {
        hashCode = generateHashCode()
    }

    fun generateHashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + fontName.hashCode()
        result = 31 * result + properties
        result = 31 * result + limits
        return result
    }

    val widthLimit get() = GFXx2D.getSizeX(limits)
    val heightLimit get() = GFXx2D.getSizeY(limits)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as TextCacheKey

        if (hashCode != other.hashCode) return false
        if (text != other.text) return false
        if (fontName != other.fontName) return false
        if (properties != other.properties) return false
        if (limits != other.limits) return false

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