package me.anno.fonts

class StringPart(
    val xPos: Float, val yPos: Float,
    val text: CharSequence, val font: AWTFont, var lineWidth: Float,
    val codepointLength: Int = text.toString().codePointCount(0, text.length)
) {
    fun plus(dy: Float) = StringPart(xPos, yPos + dy, text, font, lineWidth, codepointLength)
    override fun toString() = "{x=$xPos,y=$yPos,\"$text\",\"${font.awtFont.name}\",w=$lineWidth,cpl=$codepointLength}"
}