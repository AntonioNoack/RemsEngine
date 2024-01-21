package me.anno.fonts.signeddistfields

import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import org.joml.AABBf

class Contour(val segments: List<EdgeSegment>) {

    val bounds = AABBf()
    fun calculateBounds(): AABBf {
        bounds.clear()
        val tmp = FloatArray(2)
        for (segment in segments) segment.union(bounds, tmp)
        return bounds
    }

    companion object {

        var calculateContoursImpl: ((Font, CharSequence) -> List<Contour>)? = null
        fun calculateContours(font: Font, text: CharSequence): List<Contour> {
            return calculateContoursImpl?.invoke(font, text) ?: emptyList()
        }
    }
}