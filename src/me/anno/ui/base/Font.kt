package me.anno.ui.base

import me.anno.gpu.GFXx2D
import kotlin.math.roundToInt

data class Font(val name: String, val size: Float, val isBold: Boolean, val isItalic: Boolean) {

    val sizeInt = size.roundToInt()

    val sample = GFXx2D.getTextSize(this, "w", -1)

    override fun toString() =
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}"

    fun withBold(bold: Boolean) = Font(name, size, bold, isItalic)
    fun withItalic(italic: Boolean) = Font(name, size, isBold, italic)
    fun withName(name: String) = Font(name, size, isBold, isItalic)
    fun withSize(size: Float) = Font(name, size, isBold, isItalic)

}