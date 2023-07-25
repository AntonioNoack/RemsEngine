package me.anno.fonts.mesh

import me.anno.fonts.AWTFont
import me.anno.gpu.buffer.StaticBuffer
import me.anno.maths.Maths
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.types.Strings.joinChars
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

class AlignmentGroup(
    val font: AWTFont,
    val charDistance: KeyPairMap<Int, Int, Double>, // |a| = |ab| - |b|
    val charSize: HashMap<Int, Double>,// |a|
    val buffers: HashMap<Int, StaticBuffer> // triangles of a
) {

    fun getOffset(ctx: FontRenderContext, previous: Int, current: Int): Double {

        val alignment = this
        val map = alignment.charDistance
        val characterLengthCache = alignment.charSize

        fun getLength(str: String): Double {
            if (str.isEmpty()) return 0.0
            if (' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ", ""))
                val spacesLength = str.count { it == ' ' } * Maths.clamp(
                    getLength("x"),
                    1.0,
                    font.font.size.toDouble()
                ) * 0.667
                return lengthWithoutSpaces + spacesLength
            }
            val bounds = TextLayout(str, font.font, ctx).bounds
            return bounds.maxX
        }

        fun getCharLength(char: Int): Double {
            var value = characterLengthCache[char]
            if (value != null) return value
            value = getLength(char.joinChars().toString())
            characterLengthCache[char] = value
            return value
        }

        synchronized(alignment) {
            var offset = map[previous, current]
            if (offset != null) return offset
            val bLength = getCharLength(current)
            val abLength = getLength(previous.joinChars().toString() + current.joinChars().toString())
            offset = abLength - bLength
            map[previous, current] = offset
            return offset
        }
    }

    companion object {
        private val alignments = HashMap<AWTFont, AlignmentGroup>()
        fun getAlignments(font: AWTFont): AlignmentGroup {
            var alignment = alignments[font]
            if (alignment != null) return alignment
            alignment = AlignmentGroup(font, KeyPairMap(), HashMap(), HashMap())
            alignments[font] = alignment
            return alignment
        }
    }
}
