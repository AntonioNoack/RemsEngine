package me.anno.fonts

import java.awt.font.TextLayout
import kotlin.math.max

class PartResult(
    val parts: List<StringPart>,
    val width: Float,
    val height: Float,
    val exampleLayout: TextLayout){

    operator fun plus(s: PartResult) = PartResult(parts + s.parts.map {
        stringPart -> StringPart(stringPart.xPos + width, stringPart.yPos, stringPart.text, stringPart.font, stringPart.lineWidth)
    }, width + s.width, max(height, s.height), exampleLayout)

}