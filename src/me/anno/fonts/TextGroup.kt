package me.anno.fonts

import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup.Companion.getAlignments
import me.anno.fonts.mesh.TextRepBase
import me.anno.utils.types.Lists.accumulate
import me.anno.utils.Maths.clamp
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import kotlin.streams.toList

/**
 * custom character-character alignment maps by font for faster calculation
 * */
abstract class TextGroup(
    val font: Font, val text: String,
    var charSpacing: Float
) : TextRepBase() {

    val alignment = getAlignments(font)

    val codepoints = text.codePoints().toList()

    val offsets: List<Double>
    val baseScale: Float

    init {

        val ctx = FontRenderContext(null, true, true)
        offsets = (codepoints.mapIndexed { index, secondCodePoint ->
            if (index > 0) {
                val firstCodePoint = codepoints[index - 1]
                charSpacing + getOffset(ctx, firstCodePoint, secondCodePoint)
            } else 0.0
        } + getOffset(ctx, codepoints.last(), 32)).accumulate() // space

        if ('\t' in text || '\n' in text) throw RuntimeException("\t and \n are not allowed in FontMesh2!")
        val layout = TextLayout(".", font, ctx)
        baseScale = TextMesh.DEFAULT_LINE_HEIGHT / (layout.ascent + layout.descent)
        minX = 0f
        maxX = 0f

    }

    private fun getOffset(ctx: FontRenderContext, previous: Int, current: Int): Double {

        val map = alignment.charDistance
        val characterLengthCache = alignment.charSize

        fun getLength(str: String): Double {
            if(str.isEmpty()) return 0.0
            if(' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ",""))
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