package me.anno.fonts

import java.awt.Font

class StringPart(
    val xPos: Float,
    val yPos: Float,
    val text: String,
    val font: Font,
    var lineWidth: Float){
    val codepointLength = text.codePoints().count().toInt()
}