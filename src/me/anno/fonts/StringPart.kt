package me.anno.fonts

class StringPart(
    val xPos: Float, val yPos: Float, val font: TextGenerator,
    val text: CharSequence, var lineWidth: Float,
    val codepointLength: Int = text.toString().codePointCount(0, text.length)
) {
    fun plus(dy: Float) = StringPart(xPos, yPos + dy, font, text, lineWidth, codepointLength)
    override fun toString() = "{x=$xPos,y=$yPos,$font,\"$text\",w=$lineWidth,cpl=$codepointLength}"
}