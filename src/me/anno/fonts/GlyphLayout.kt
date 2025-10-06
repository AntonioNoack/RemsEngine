package me.anno.fonts

import me.anno.fonts.mesh.CharacterOffsetCache.Companion.getOffsetCache
import org.joml.AABBf

// todo use these parts instead of our custom logic
// todo the same for any SDFs

open class GlyphLayout(
    val font: Font, val text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int
) : GlyphList(text.length), TextDrawable {

    private val offsetCache = getOffsetCache(font)

    override val bounds = AABBf()

    val width: Float
    val height: Float
    val actualFontSize: Float
    val baseScale: Float
    val numLines: Int

    init {
        val parts = FontManager.getFont(font).splitParts(
            text, font.size,
            font.relativeTabSize, font.relativeCharSpacing,
            relativeWidthLimit, maxNumLines
        )

        width = parts.width
        height = parts.height
        numLines = parts.numLines
        actualFontSize = parts.actualFontSize
        baseScale = 1f / font.size
        findGlyphs(parts.parts)

        // are these correct??
        bounds.union(0f, 0f, 0f)
        bounds.union(width * baseScale, height * baseScale, 0f)
    }

    val meshCache get() = offsetCache.charMesh

    private fun findGlyphs(parts: List<StringPart>) {
        val charSpacing = baseScale * font.relativeCharSpacing
        for (partIndex in parts.indices) {
            val part = parts[partIndex]
            var offset1 = part.xPos
            val codepoints = part.codepoints
            for (i in part.i0 until part.i1) {

                val currC = codepoints[i]
                val offset0 = offset1
                val nextC = if (i + 1 < part.i1) codepoints[i] else ' '.code
                offset1 += charSpacing + offsetCache.getOffset(currC, nextC)

                add(currC, offset0, offset1, part.yPos, part.lineWidth)
            }
        }
    }

    override fun destroy() {
        // nothing to do
    }

    /**
     * return true when done
     * */
    fun draw(drawBuffer: DrawBufferCallback) {
        draw(0, size, drawBuffer)
    }

    /**
     * return true when done
     * */
    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        // not implemented
    }
}