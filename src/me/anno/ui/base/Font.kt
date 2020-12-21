package me.anno.ui.base

data class Font(val name: String, val size: Float, val isBold: Boolean, val isItalic: Boolean) {

    override fun toString() =
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}"

    fun withBold(bold: Boolean) = Font(name, size, bold, isItalic)
    fun withItalic(italic: Boolean) = Font(name, size, isBold, italic)
    fun withName(name: String) = Font(name, size, isBold, isItalic)
    fun withSize(size: Float) = Font(name, size, isBold, isItalic)

}