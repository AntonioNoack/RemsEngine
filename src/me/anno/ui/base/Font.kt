package me.anno.ui.base

import me.anno.fonts.FontManager
import me.anno.gpu.GFXx2D
import kotlin.math.roundToInt

data class Font(val name: String, val size: Float, val isBold: Boolean, val isItalic: Boolean) {

    constructor(name: String, size: Int, isBold: Boolean, isItalic: Boolean):
            this(name, size.toFloat(), isBold, isItalic)

    val sizeInt = size.roundToInt()
    val sizeIndex = FontManager.getFontSizeIndex(size)

    val sampleSize = GFXx2D.getTextSize(this, "w", -1)
    val sampleWidth = GFXx2D.getSizeX(sampleSize)
    val sampleHeight = GFXx2D.getSizeY(sampleSize)

    fun withBold(bold: Boolean) = Font(name, size, bold, isItalic)
    fun withItalic(italic: Boolean) = Font(name, size, isBold, italic)
    fun withName(name: String) = Font(name, size, isBold, isItalic)
    fun withSize(size: Float) = Font(name, size, isBold, isItalic)

    override fun toString() =
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}"

}