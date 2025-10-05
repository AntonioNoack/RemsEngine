package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Codepoints.markEmojisInCodepoints
import me.anno.fonts.mesh.CharacterOffsetCache
import me.anno.fonts.mesh.CharacterOffsetCache.Companion.getOffsetCache
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.DoubleArrays.accumulate

open class TextGroup(val font: Font, val text: CharSequence, charSpacing: Double) : TextDrawable() {

    companion object {
        val someSingleEmoji = "\uD83D\uDCC1".codepoints().apply {
            assertEquals(1, size)
        }.first()
    }

    private val offsetCache: CharacterOffsetCache = getOffsetCache(font)

    val codepoints = text
        .codepoints()
        .markEmojisInCodepoints()

    val offsets = DoubleArray(codepoints.size + 1)
    val baseScale = 1.0 / font.size

    init {

        if (codepoints.isNotEmpty()) {

            var prevC = -1

            fun push(currC: Int, currI: Int) {
                if (prevC >= 0) {
                    offsets[currI] = charSpacing + offsetCache.getOffset(prevC, currC)
                }
                prevC = currC
            }

            for (currI in codepoints.indices) {
                var currC = codepoints[currI]
                if (currC >= 0) {
                    if (currI > 0 && codepoints[currI - 1] < 0) {
                        currC = someSingleEmoji
                    }
                    push(currC, currI)
                }
            }
            push(' '.code, codepoints.size)

            offsets.accumulate()
        }

        bounds.minX = 0f
        bounds.maxX = 0f
    }

    val meshCache get() = offsetCache.charMesh

    override fun destroy() {
        // nothing to do
    }

    /**
     * return true when done
     * */
    fun draw(drawBuffer: DrawBufferCallback) {
        draw(0, codepoints.size, drawBuffer)
    }

    /**
     * return true when done
     * */
    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        // not implemented
    }
}