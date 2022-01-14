package me.anno.fonts

import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup.Companion.getAlignments
import me.anno.fonts.mesh.TextRepBase
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.arrays.DoubleArrays.accumulate
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

/**
 * custom character-character alignment maps by font for faster calculation
 * */
abstract class TextGroup(
    val font: Font,
    val text: CharSequence,
    charSpacing: Double
) : TextRepBase() {

    val alignment = getAlignments(font)

    val codepoints: IntArray =
        text.codePoints().toArray()

    val offsets: DoubleArray
    val baseScale: Double

    init {

        val ctx = FontRenderContext(null, true, true)
        offsets = DoubleArray(codepoints.size + 1)
        for (index in 1 until codepoints.size) {
            val firstCodePoint = codepoints[index - 1]
            val secondCodePoint = codepoints[index]
            offsets[index] = charSpacing + getOffset(ctx, firstCodePoint, secondCodePoint)
        }
        offsets[codepoints.size] = getOffset(ctx, codepoints.last(), 32)
        offsets.accumulate()

        if ('\t' in text || '\n' in text) throw RuntimeException("\t and \n are not allowed in FontMesh2!")
        val layout = TextLayout(".", font, ctx)
        baseScale = TextMesh.DEFAULT_LINE_HEIGHT.toDouble() / (layout.ascent + layout.descent)
        minX = 0f
        maxX = 0f

    }

    private fun getOffset(ctx: FontRenderContext, previous: Int, current: Int): Double {

        val map = alignment.charDistance
        val characterLengthCache = alignment.charSize

        fun getLength(str: String): Double {
            if (str.isEmpty()) return 0.0
            if (' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ", ""))
                val spacesLength = str.count { it == ' ' } * clamp(
                    getLength("x"),
                    1.0,
                    font.size.toDouble()
                ) * 0.667
                return lengthWithoutSpaces + spacesLength
            }
            return TextLayout(str, font, ctx).bounds.maxX
        }

        fun getCharLength(char: Int): Double {
            var value = characterLengthCache[char]
            if (value != null) return value
            value = getLength(String(Character.toChars(char)))
            characterLengthCache[char] = value
            return value
        }

        synchronized(alignment) {
            var offset = map[previous, current]
            if (offset != null) return offset
            val bLength = getCharLength(current)
            val abLength = getLength(String(Character.toChars(previous) + Character.toChars(current)))
            offset = abLength - bLength
            map[previous, current] = offset
            return offset
        }

    }

    override fun destroy() {

    }

}