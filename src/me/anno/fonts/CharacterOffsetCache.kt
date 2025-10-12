package me.anno.fonts

import me.anno.maths.Maths
import me.anno.maths.Packing
import speiger.primitivecollections.LongToDoubleHashMap

class CharacterOffsetCache(val font: Font) {

    private val fontImpl = FontManager.getFontImpl()
    private val charDistance = LongToDoubleHashMap(0.0)// |a| = |ab| - |b|
    private val charWidth = LongToDoubleHashMap(0.0)// |a|

    val spaceWidth by lazy {
        val xLength = fontImpl.getTextLength(font, 'o'.code)
        Maths.clamp(xLength, 1f, font.size) * 0.667f
    }

    val emojiSize = font.sizeInt.toFloat()
    val emojiPadding = emojiSize * IEmojiCache.emojiPadding

    fun getCharLength(codepoint: Int): Float {
        if (codepoint == ' '.code) return spaceWidth
        return charWidth.getOrPut(codepoint.toLong()) {
            fontImpl.getTextLength(font, codepoint).toDouble()
        }.toFloat()
    }

    fun getLength(codepointA: Int, codepointB: Int): Float {
        return if (codepointA == ' '.code || codepointB == ' '.code) {
            getCharLength(codepointA) + getCharLength(codepointB)
        } else fontImpl.getTextLength(font, codepointA, codepointB)
    }

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(codepointA: Int, codepointB: Int): Float {
        return synchronized(this) {
            charDistance.getOrPut(Packing.pack64(codepointA, codepointB)) {
                val ai = Codepoints.isEmoji(codepointA)
                val bi = Codepoints.isEmoji(codepointB)
                when {
                    ai -> emojiSize + emojiPadding
                    bi -> getCharLength(codepointA) + emojiPadding
                    codepointA == ' '.code -> spaceWidth
                    codepointB == ' '.code -> getCharLength(codepointA)
                    else -> {
                        val bLength = getCharLength(codepointB)
                        val abLength = getLength(codepointA, codepointB)
                        abLength - bLength
                    }
                }.toDouble()
            }.toFloat()
        }
    }

    companion object {

        private val caches = HashMap<Font, CharacterOffsetCache>()
        fun getOffsetCache(font: Font): CharacterOffsetCache {
            return synchronized(caches) {
                caches.getOrPut(font) {
                    CharacterOffsetCache(font)
                }
            }
        }
    }
}