package me.anno.fonts

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths
import me.anno.maths.Packing
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.joinChars0
import speiger.primitivecollections.IntToObjectHashMap
import speiger.primitivecollections.LongToDoubleHashMap

class CharacterOffsetCache(val font: Font) {

    private val charDistance = LongToDoubleHashMap(0.0)// |a| = |ab| - |b|
    private val charWidth = LongToDoubleHashMap(0.0)// |a|
    val charMesh = IntToObjectHashMap<Mesh>() // triangles of a

    val spaceLength by lazy {
        val xLength = FontStats.getTextLength(font, "x")
        Maths.clamp(xLength, 1.0, font.size.toDouble()) * 0.667
    }

    val emojiSize = font.sizeInt.toDouble()
    val emojiPadding = emojiSize * IEmojiCache.emojiPadding

    /**
     * get |AB| - |B| aka, the length of A when standing before B
     * */
    fun getOffset(charA: Int, charB: Int): Float {

        fun getLength(str: String): Double {
            if (str.isEmpty()) return 0.0
            if (' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ", ""))
                val spacesLength = str.count { it == ' ' } * spaceLength
                return lengthWithoutSpaces + spacesLength
            }
            return FontStats.getTextLength(font, str)
        }

        fun getCharLength(char: Int): Double {
            return charWidth.getOrPut(char.toLong()) {
                getLength(char.joinChars())
            }
        }

        return synchronized(this) {
            charDistance.getOrPut(Packing.pack64(charA, charB)) {
                val ai = Codepoints.isEmoji(charA)
                val bi = Codepoints.isEmoji(charB)
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