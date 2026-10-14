package me.anno.fonts

import me.anno.maths.Maths
import me.anno.utils.types.Floats.toIntOr

object CharacterOffsets {

    fun getSpaceWidth(font: Font, fontImpl: FontImpl<*>): Int {
        val xLength = fontImpl.getTextLength(font, 'o'.code)
        return Maths.clamp(xLength, 1, font.sizeInt) * 2 / 3
    }

    private fun emojiSize(font: Font) = font.sizeInt
    private fun emojiPadding(font: Font) = (emojiSize(font) * IEmojiCache.emojiPadding).toIntOr()

    fun getCharLength(font: Font, fontImpl: FontImpl<*>, codepoint: Int): Int {
        if (codepoint == ' '.code) return getSpaceWidth(font, fontImpl)
        return fontImpl.getTextLength(font, codepoint)
    }

    fun getLength(
        font: Font, fontImpl: FontImpl<*>,
        codepointA: Int, codepointB: Int,
    ): Int {
        return if (codepointA == ' '.code || codepointB == ' '.code) {
            getCharLength(font, fontImpl, codepointA) + getCharLength(font, fontImpl, codepointB)
        } else fontImpl.getTextLength(font, codepointA, codepointB)
    }

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(
        font: Font, fontImpl: FontImpl<*>,
        codepointA: Int, codepointB: Int,
    ): Int {
        val ai = Codepoints.isEmoji(codepointA)
        val bi = Codepoints.isEmoji(codepointB)
        return when {
            ai -> emojiSize(font) + emojiPadding(font)
            bi -> getCharLength(font, fontImpl, codepointA) + emojiPadding(font)
            codepointA == ' '.code -> getSpaceWidth(font, fontImpl)
            codepointB == ' '.code -> getCharLength(font, fontImpl, codepointA)
            else -> {
                val bLength = getCharLength(font, fontImpl, codepointB)
                val abLength = getLength(font, fontImpl, codepointA, codepointB)
                abLength - bLength
            }
        }
    }

}