package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.Font
import me.anno.fonts.FontStats.getTextLength
import me.anno.maths.Maths
import me.anno.maths.Packing.pack64
import me.anno.utils.types.Strings.joinChars
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

    fun getOffset(previous: Int, current: Int): Double {

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
                getLength(char.joinChars().toString())
            }
        }

        return synchronized(this) {
            charDistance.getOrPut(pack64(previous, current)) {
                val bLength = getCharLength(current)
                val abLength = getLength(previous.joinChars().toString() + current.joinChars().toString())
                abLength - bLength
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
