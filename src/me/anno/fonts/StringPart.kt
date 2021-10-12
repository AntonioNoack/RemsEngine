package me.anno.fonts

import org.joml.Vector2f
import java.awt.Font

class StringPart(
    val xPos: Float,
    val yPos: Float,
    val text: String,
    val font: Font,
    var lineWidth: Float,
    val codepointLength: Int = text.codePointCount(0, text.length)
) {
    fun plus(dy: Float) = StringPart(
        xPos, yPos + dy, text, font, lineWidth, codepointLength
    )
}