package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.mesh.CharacterOffsetCache
import me.anno.fonts.mesh.CharacterOffsetCache.Companion.getOffsetCache
import me.anno.utils.structures.arrays.DoubleArrays.accumulate

open class TextGroup(val font: Font, val text: CharSequence, charSpacing: Double) : TextDrawable() {

    private val offsetCache: CharacterOffsetCache = getOffsetCache(font)

    val codepoints = text.codepoints()
    val offsets = DoubleArray(codepoints.size + 1)
    val baseScale = 1.0 / font.size

    init {

        if (codepoints.isNotEmpty()) {
            var firstCodePoint = codepoints[0]
            if (firstCodePoint == '\t'.code || firstCodePoint == '\n'.code) firstCodePoint = ' '.code
            for (index in 1 until codepoints.size) {
                var secondCodePoint = codepoints[index]
                if (secondCodePoint == '\t'.code || secondCodePoint == '\n'.code) secondCodePoint = ' '.code
                offsets[index] = charSpacing + offsetCache.getOffset(firstCodePoint, secondCodePoint)
                firstCodePoint = secondCodePoint
            }
            offsets[codepoints.size] = offsetCache.getOffset(codepoints.last(), 32)
            offsets.accumulate()
        }

        bounds.minX = 0f
        bounds.maxX = 0f
    }

    val meshCache get() = offsetCache.charMesh

    override fun destroy() {
        // nothing to do
    }

    fun draw(drawBuffer: DrawBufferCallback) {
        draw(0, codepoints.size, drawBuffer)
    }

    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        // not implemented
    }
}