package me.anno.ui.base

import me.anno.fonts.FontManager
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.DrawTexts
import kotlin.math.roundToInt

data class Font(val name: String, val size: Float, val isBold: Boolean, val isItalic: Boolean) {

    constructor(name: String, size: Int, isBold: Boolean, isItalic: Boolean):
            this(name, size.toFloat(), isBold, isItalic)

    val sizeInt = size.roundToInt()
    val sizeIndex = FontManager.getFontSizeIndex(size)

    class SampleSize(font: Font){
        val size = DrawTexts.getTextSize(font, "w", -1, -1)
        val width = GFXx2D.getSizeX(size)
        val height = GFXx2D.getSizeY(size)
    }

    val sample = lazy { SampleSize(this) }
    val sampleWidth get() = sample.value.width
    val sampleHeight get() = sample.value.height
    val sampleSize get() = sample.value.size

    fun withBold(bold: Boolean) = Font(name, size, bold, isItalic)
    fun withItalic(italic: Boolean) = Font(name, size, isBold, italic)
    fun withName(name: String) = Font(name, size, isBold, isItalic)
    fun withSize(size: Float) = Font(name, size, isBold, isItalic)

    override fun toString() =
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}"

}