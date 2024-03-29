package me.anno.fonts

import me.anno.utils.Logging.hash32
import kotlin.math.max

class PartResult(val parts: List<StringPart>, val width: Float, val height: Float, val lineCount: Int) {

    @Suppress("unused") // I think this is used in Rem's Studio
    operator fun plus(s: PartResult) = PartResult(parts + s.parts.map { stringPart ->
        StringPart(stringPart.xPos + width, stringPart.yPos, stringPart.font, stringPart.text, stringPart.lineWidth)
    }, width + s.width, max(height, s.height), lineCount)

    override fun toString(): String {
        return "PartResult@${hash32(this)}{w=$width,h=$height,lc=$lineCount,p=[${parts.joinToString()}]}"
    }
}