package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.Font
import me.anno.fonts.FontStats.getTextLength
import me.anno.fonts.IEmojiCache
import me.anno.maths.Maths
import me.anno.maths.Packing.pack64
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.joinChars0
import speiger.primitivecollections.IntToObjectHashMap
import speiger.primitivecollections.LongToDoubleHashMap

class CharacterOffsetCache(val font: Font) {

    private val charDistance = LongToDoubleHashMap(0.0)// |a| = |ab| - |b|
    private val charWidth = LongToDoubleHashMap(0.0)// |a|
    val charMesh = IntToObjectHashMap<Mesh>() // triangles of a

    val spaceLength by lazy {
        val xLength = getTextLength(font, "x")
        Maths.clamp(xLength, 1.0, font.size.toDouble()) * 0.667
    }

    val emojiSize = font.sizeInt.toDouble()
    val emojiPadding = emojiSize * IEmojiCache.emojiPadding

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(charA: Int, charB: Int): Double {

        fun getLength(str: String): Double {
            if (str.isEmpty()) return 0.0
            if (' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ", ""))
                val spacesLength = str.count { it == ' ' } * spaceLength
                return lengthWithoutSpaces + spacesLength
            }
            return getTextLength(font, str)
        }

        fun getCharLength(char: Int): Double {
            return charWidth.getOrPut(char.toLong()) {
                getLength(char.joinChars())
            }
        }

        return synchronized(this) {
            charDistance.getOrPut(pack64(charA, charB)) {
                val ai = isEmoji(charA)
                val bi = isEmoji(charB)
                when {
                    ai && bi -> emojiSize + emojiPadding
                    ai -> emojiSize + emojiPadding
                    bi -> getCharLength(charA) + emojiPadding
                    else -> {
                        val bLength = getCharLength(charB)
                        val abLength = getLength(String(charA.joinChars0() + charB.joinChars0()))
                        abLength - bLength
                    }
                }
            }
        }
    }

    companion object {

        private fun isEmoji(codepoint: Int): Boolean {
            return IEmojiCache.emojiCache.contains(codepoint)
        }

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
