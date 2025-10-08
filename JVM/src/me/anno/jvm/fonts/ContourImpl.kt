package me.anno.jvm.fonts

import me.anno.fonts.Codepoints
import me.anno.fonts.IEmojiCache
import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.Contours
import me.anno.fonts.signeddistfields.edges.CubicSegment
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.utils.Color
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator

object ContourImpl {

    private val LOGGER = LogManager.getLogger(ContourImpl::class)

    private fun getAWTFont(font: me.anno.fonts.Font): Font {
        return FontManagerImpl.getAWTFont(font).awtFont
    }

    fun calculateContours(font: me.anno.fonts.Font, codepoint: Int): Contours {
        val emojiContours = getEmojiContours(font, codepoint)
        if (emojiContours != null) return emojiContours
        return calculateContours(getAWTFont(font), codepoint.joinChars())
    }

    private fun getEmojiContours(font: me.anno.fonts.Font, codepoint: Int): Contours? {
        return if (Codepoints.isEmoji(codepoint)) {
            val emojiId = Codepoints.getEmojiId(codepoint)
            IEmojiCache.emojiCache.getEmojiContours(emojiId, font.sizeInt).waitFor()
        } else null
    }

    private fun calculateContours(font: Font, text: String): Contours {

        val contours = ArrayList<Contour>()
        val segments = ArrayList<EdgeSegment>()

        val ctx = FontRenderContext(null, true, true)

        val shape = GeneralPath()
        val layout = TextLayout(text, font, ctx)

        val outline = layout.getOutline(null)
        shape.append(outline, true)

        val path = shape.getPathIterator(null)

        var p0 = Vector2f()
        val coordinates = FloatArray(6)
        while (!path.isDone) {

            val type = path.currentSegment(coordinates)

            val x1 = coordinates[0]
            val y1 = coordinates[1]

            val x2 = coordinates[2]
            val y2 = coordinates[3]

            val x3 = coordinates[4]
            val y3 = coordinates[5]

            when (type) {
                PathIterator.SEG_LINETO -> {
                    val p1 = Vector2f(x1, y1)
                    segments.add(LinearSegment(p0, p1))
                    p0 = p1
                }
                PathIterator.SEG_QUADTO -> {
                    val p2 = Vector2f(x2, y2)
                    segments.add(QuadraticSegment(p0, Vector2f(x1, y1), p2))
                    p0 = p2
                }
                PathIterator.SEG_CUBICTO -> {
                    val p3 = Vector2f(x3, y3)
                    segments.add(CubicSegment(p0, Vector2f(x1, y1), Vector2f(x2, y2), p3))
                    p0 = p3
                }
                PathIterator.SEG_MOVETO -> {
                    if (segments.isNotEmpty()) {
                        LOGGER.warn("move to is only allowed after close or at the start...")
                    }
                    p0 = Vector2f(x1, y1) // is one move too much...
                }
                PathIterator.SEG_CLOSE -> {
                    if (segments.isNotEmpty()) {
                        val color = Color.white
                        contours.add(Contour(ArrayList(segments), 0f, color))
                    }
                    segments.clear()
                }
            }

            path.next()
        }
        return Contours(contours, true)
    }
}