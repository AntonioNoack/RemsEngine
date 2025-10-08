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

    fun getLength(charA: Int, charB: Int): Float {
        return if (charA == ' '.code || charB == ' '.code) {
            getCharLength(charA) + getCharLength(charB)
        } else fontImpl.getTextLength(font, charA, charB)
    }

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(charA: Int, charB: Int): Float {
        return synchronized(this) {
            charDistance.getOrPut(Packing.pack64(charA, charB)) {
                val ai = Codepoints.isEmoji(charA)
                val bi = Codepoints.isEmoji(charB)
                when {
                    ai -> emojiSize + emojiPadding
                    bi -> getCharLength(charA) + emojiPadding
                    charA == ' '.code -> spaceWidth
                    charB == ' '.code -> getCharLength(charA)
                    else -> {
                        val bLength = getCharLength(charB)
                        val abLength = getLength(charA, charB)
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