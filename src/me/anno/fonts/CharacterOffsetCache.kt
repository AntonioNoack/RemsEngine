package me.anno.fonts

import me.anno.maths.Maths
import me.anno.maths.Packing
import me.anno.utils.types.Floats.toIntOr
import speiger.primitivecollections.IntToIntHashMap
import speiger.primitivecollections.LongToDoubleHashMap
import speiger.primitivecollections.LongToIntHashMap

class CharacterOffsetCache(val font: Font) {

    private val fontImpl = FontManager.getFontImpl()
    private val charDistance = LongToIntHashMap(0)// |a| = |ab| - |b|
    private val charWidth = IntToIntHashMap(0)// |a|

    val spaceWidth by lazy {
        val xLength = fontImpl.getTextLength(font, 'o'.code)
        Maths.clamp(xLength, 1, font.sizeInt) * 2 / 3
    }

    val emojiSize = font.sizeInt
    val emojiPadding = (emojiSize * IEmojiCache.emojiPadding).toIntOr()

    fun getCharLength(codepoint: Int): Int {
        if (codepoint == ' '.code) return spaceWidth
        return charWidth.getOrPut(codepoint) {
            fontImpl.getTextLength(font, codepoint)
        }
    }

    fun getLength(codepointA: Int, codepointB: Int): Int {
        return if (codepointA == ' '.code || codepointB == ' '.code) {
            getCharLength(codepointA) + getCharLength(codepointB)
        } else fontImpl.getTextLength(font, codepointA, codepointB)
    }

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(codepointA: Int, codepointB: Int): Int {
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
                }
            }
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