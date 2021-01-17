package me.anno.cache.keys

data class TextSegmentKey(
    val font: java.awt.Font, val isBold: Boolean, val isItalic: Boolean,
    val text: String, val charSpacing: Float
) {
    fun equals(isBold: Boolean, isItalic: Boolean, text: String, charSpacing: Float) =
        isBold == this.isBold && isItalic == this.isItalic && text == this.text && charSpacing == this.charSpacing
}