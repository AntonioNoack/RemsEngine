package me.anno.fonts

import me.anno.fonts.mesh.AlignmentGroup.Companion.getAlignments
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextRepBase
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.arrays.DoubleArrays.accumulate
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

/**
 * custom character-character alignment maps by font for faster calculation
 * */
open class TextGroup(
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
        var firstCodePoint = codepoints[0]
        if (firstCodePoint == '\t'.code || firstCodePoint == '\n'.code) firstCodePoint = ' '.code
        for (index in 1 until codepoints.size) {
            var secondCodePoint = codepoints[index]
            if (secondCodePoint == '\t'.code || secondCodePoint == '\n'.code) secondCodePoint = ' '.code
            offsets[index] = charSpacing + getOffset(ctx, firstCodePoint, secondCodePoint)
            firstCodePoint = secondCodePoint
        }
        offsets[codepoints.size] = getOffset(ctx, codepoints.last(), 32)
        offsets.accumulate()

        val layout = TextLayout(".", font, ctx)
        baseScale = TextMesh.DEFAULT_LINE_HEIGHT.toDouble() / (layout.ascent + layout.descent)
        minX = 0f
        maxX = 0f

    }

    private fun getOffset(ctx: FontRenderContext, previous: Int, current: Int) =
        alignment.getOffset(ctx, previous, current)

    override fun destroy() {

    }

    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit) {
        throw NotImplementedError()
    }

}