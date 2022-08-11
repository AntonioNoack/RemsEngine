package me.anno.fonts

import java.awt.Font

class StringPart(
    val xPos: Float, val yPos: Float,
    val text: CharSequence, val font: Font, var lineWidth: Float,
    val codepointLength: Int = text.toString().codePointCount(0, text.length)
) {
    fun plus(dy: Float) = StringPart(xPos, yPos + dy, text, font, lineWidth, codepointLength)
}