package me.anno.fonts

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths
import me.anno.maths.Packing
import speiger.primitivecollections.IntToObjectHashMap
import speiger.primitivecollections.LongToDoubleHashMap

class CharacterOffsetCache(val font: Font) {

    private val charDistance = LongToDoubleHashMap(0.0)// |a| = |ab| - |b|
    private val charWidth = LongToDoubleHashMap(0.0)// |a|
    val charMesh = IntToObjectHashMap<Mesh>() // triangles of a

    val spaceWidth by lazy {
        val xLength = FontStats.getTextLength(font, 'x'.code)
        Maths.clamp(xLength, 1.0, font.size.toDouble()) * 0.667
    }

    val emojiSize = font.sizeInt.toDouble()
    val emojiPadding = emojiSize * IEmojiCache.emojiPadding

    fun getCharLength(codepoint: Int): Double {
        if (codepoint == ' '.code) return spaceWidth
        return charWidth.getOrPut(codepoint.toLong()) {
            return FontStats.getTextLength(font, codepoint)
        }
    }

    fun getLength(charA: Int, charB: Int): Double {
        return if (charA == ' '.code || charB == ' '.code) {
            getCharLength(charA) + getCharLength(charB)
        } else FontStats.getTextLength(font, charA, charB)
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
                }
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