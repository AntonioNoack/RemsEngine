package me.anno.fonts

import me.anno.fonts.Codepoints.countCodepoints
import me.anno.image.Image

class StringPart(
    val xPos: Float, val yPos: Float, val font: TextGenerator,
    val text: CharSequence, var lineWidth: Float,
    val bitmap: Image?,
    val codepointLength: Int = text.countCodepoints()
) {
    fun plus(dy: Float) = StringPart(xPos, yPos + dy, font, text, lineWidth, bitmap, codepointLength)
    override fun toString() = "{x=$xPos,y=$yPos,$font,\"$text\",w=$lineWidth,cpl=$codepointLength}"
}