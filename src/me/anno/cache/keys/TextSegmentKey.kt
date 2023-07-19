package me.anno.cache.keys

import me.anno.fonts.AWTFont

@Suppress("unused")
data class TextSegmentKey(
    val font: AWTFont,
    val isBold: Boolean, val isItalic: Boolean,
    val text: CharSequence, val charSpacing: Float
) {

    fun equals(isBold: Boolean, isItalic: Boolean, text: CharSequence, charSpacing: Float) =
        isBold == this.isBold && isItalic == this.isItalic && text == this.text && charSpacing == this.charSpacing

    private val _hashCode = generateHashCode()

    override fun hashCode(): Int {
        return _hashCode
    }

    private fun generateHashCode(): Int {
        var result = font.name.hashCode()
        result = 31 * result + font.size.hashCode()
        result = 31 * result + isBold.hashCode()
        result = 31 * result + isItalic.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + charSpacing.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextSegmentKey) return false

        if (_hashCode != other._hashCode) return false
        val font2 = other.font
        if (font.name != font2.name) return false
        if (font.size != font2.size) return false
        if (isBold != other.isBold) return false
        if (isItalic != other.isItalic) return false
        if (text != other.text) return false
        if (charSpacing != other.charSpacing) return false

        return true
    }
}