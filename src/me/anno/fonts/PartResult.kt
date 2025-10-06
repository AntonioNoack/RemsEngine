package me.anno.fonts

import me.anno.utils.Logging.hash32

class PartResult(
    val parts: ArrayList<StringPart>,
    val actualFontSize: Float,
    var width: Float, var height: Float,
    var numLines: Int
) {
    override fun toString(): String {
        return "PartResult@${hash32(this)}{w=$width,h=$height,lc=$numLines,p=[${parts.joinToString()}]}"
    }
}