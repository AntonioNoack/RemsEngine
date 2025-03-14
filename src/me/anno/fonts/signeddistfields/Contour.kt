package me.anno.fonts.signeddistfields

import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.utils.InternalAPI
import org.joml.AABBf

class Contour(val segments: List<EdgeSegment>) {

    val bounds = AABBf()
    fun calculateBounds(tmp: FloatArray): AABBf {
        bounds.clear()
        for (segment in segments) {
            segment.union(bounds, tmp)
        }
        return bounds
    }

    companion object {
        @InternalAPI
        var calculateContoursImpl: ((Font, CharSequence) -> List<Contour>)? = null
        fun calculateContours(font: Font, text: CharSequence): List<Contour> {
            return calculateContoursImpl?.invoke(font, text) ?: emptyList()
        }
    }
}