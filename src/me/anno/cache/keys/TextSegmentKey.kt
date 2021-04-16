package me.anno.cache.keys

data class TextSegmentKey(
    val font: java.awt.Font,
    val isBold: Boolean, val isItalic: Boolean,
    val text: String, val charSpacing: Float
) {

    fun equals(isBold: Boolean, isItalic: Boolean, text: String, charSpacing: Float) =
        isBold == this.isBold && isItalic == this.isItalic && text == this.text && charSpacing == this.charSpacing

    val hashCode = generateHashCode()

    override fun hashCode(): Int {
        return hashCode
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
        if (javaClass != other?.javaClass) return false

        other as TextSegmentKey

        if (hashCode != other.hashCode) return false
        if (font.name != other.font.name) return false
        if (font.size != other.font.size) return false
        if (isBold != other.isBold) return false
        if (isItalic != other.isItalic) return false
        if (text != other.text) return false
        if (charSpacing != other.charSpacing) return false

        return true
    }
}