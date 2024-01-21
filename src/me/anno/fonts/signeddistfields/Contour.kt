package me.anno.fonts.signeddistfields

import me.anno.fonts.FontManager
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import org.joml.AABBf
import java.awt.Font

class Contour(val segments: ArrayList<EdgeSegment>) {
    val bounds = AABBf()
    fun calculateBounds(): AABBf {
        bounds.clear()
        val tmp = FloatArray(2)
        for (segment in segments) segment.union(bounds, tmp)
        return bounds
    }

    companion object {

        fun calculateContours(font: me.anno.fonts.Font, text: CharSequence): List<Contour> {
            return calculateContours(FontManager.getFont(font).awtFont, text)
        }

        var calculateContoursImpl: ((Font, CharSequence) -> List<Contour>)? = null
        fun calculateContours(font: Font, text: CharSequence): List<Contour> {
            return calculateContoursImpl?.invoke(font, text) ?: emptyList()
        }
    }
}