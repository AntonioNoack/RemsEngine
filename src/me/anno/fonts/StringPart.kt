package me.anno.fonts

import me.anno.fonts.Codepoints.countCodepoints

class StringPart(
    val xPos: Float, val yPos: Float, val font: TextGenerator,
    val text: CharSequence, var lineWidth: Float,
    val codepointLength: Int = text.countCodepoints()
) {
    fun plus(dy: Float) = StringPart(xPos, yPos + dy, font, text, lineWidth, codepointLength)
    override fun toString() = "{x=$xPos,y=$yPos,$font,\"$text\",w=$lineWidth,cpl=$codepointLength}"
}