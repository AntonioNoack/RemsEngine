package me.anno.fonts

import me.anno.utils.types.Strings.joinChars

class StringPart private constructor(
    val xPos: Float, val yPos: Float, val font: TextGenerator,
    val codepoints: IntArray, val text: CharSequence,
    var lineWidth: Float,
    val i0: Int, val i1: Int, val lineIndex: Int,
) {

    val firstCodepoint: Int get() = codepoints[i0]
    val isEmoji: Boolean get() = Codepoints.isEmoji(firstCodepoint)

    companion object {
        fun fromChars(
            xPos: Float, yPos: Float, font: TextGenerator,
            lineWidth: Float,
            codepoints: IntArray, i0: Int, i1: Int,
            lineIndex: Int,
        ) = StringPart(
            xPos, yPos, font, codepoints, codepoints.joinChars(i0, i1),
            lineWidth, i0, i1, lineIndex
        )
    }

    override fun toString(): String {
        val cp0 = codepoints[i0]
        return if (Codepoints.isEmoji(cp0)) {
            "{x=$xPos,y=$yPos,Emoji#${Codepoints.getEmojiId(cp0)},\"$text\",w=$lineWidth,cpl=$i0-$i1}"
        } else {
            "{x=$xPos,y=$yPos,$font,\"$text\",w=$lineWidth,cpl=$i0-$i1}"
        }
    }
}