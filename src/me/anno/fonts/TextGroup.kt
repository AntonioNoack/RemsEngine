package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.mesh.CharacterOffsetCache
import me.anno.fonts.mesh.CharacterOffsetCache.Companion.getOffsetCache
import me.anno.fonts.mesh.TextMesh
import me.anno.ui.base.Font
import me.anno.utils.structures.arrays.DoubleArrays.accumulate
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

open class TextGroup(val font: Font, val text: CharSequence, charSpacing: Double) : TextDrawable() {

    private val offsetCache: CharacterOffsetCache = getOffsetCache(font)

    val codepoints: IntArray = text.codepoints()
    val offsets: DoubleArray = DoubleArray(codepoints.size + 1)
    val baseScale: Double

    init {

        fun getOffset(previous: Int, current: Int) =
            offsetCache.getOffset(previous, current)

        var firstCodePoint = codepoints[0]
        if (firstCodePoint == '\t'.code || firstCodePoint == '\n'.code) firstCodePoint = ' '.code
        for (index in 1 until codepoints.size) {
            var secondCodePoint = codepoints[index]
            if (secondCodePoint == '\t'.code || secondCodePoint == '\n'.code) secondCodePoint = ' '.code
            offsets[index] = charSpacing + getOffset(firstCodePoint, secondCodePoint)
            firstCodePoint = secondCodePoint
        }
        offsets[codepoints.size] = getOffset(codepoints.last(), 32)
        offsets.accumulate()

        val ctx = FontRenderContext(null, true, true)
        val layout = TextLayout(".", FontManager.getFont(font).awtFont, ctx)
        baseScale = TextMesh.DEFAULT_LINE_HEIGHT.toDouble() / (layout.ascent + layout.descent)
        bounds.minX = 0f
        bounds.maxX = 0f
    }

    val meshCache get() = offsetCache.charMesh

    override fun destroy() {
    }

    fun draw(drawBuffer: DrawBufferCallback) {
        draw(0, codepoints.size, drawBuffer)
    }

    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        throw NotImplementedError()
    }
}