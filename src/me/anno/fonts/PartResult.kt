package me.anno.fonts

import me.anno.utils.Color.hex32
import java.awt.font.TextLayout
import kotlin.math.max

class PartResult(
    val parts: List<StringPart>,
    val width: Float,
    val height: Float,
    val lineCount: Int
) {

    operator fun plus(s: PartResult) = PartResult(parts + s.parts.map { stringPart ->
        StringPart(stringPart.xPos + width, stringPart.yPos, stringPart.text, stringPart.font, stringPart.lineWidth)
    }, width + s.width, max(height, s.height), lineCount)

    override fun toString(): String {
        return "PartResult@${
            hex32(System.identityHashCode(this))
        }{w=$width,h=$height,lc=$lineCount,p=[${parts.joinToString()}]}"
    }

}