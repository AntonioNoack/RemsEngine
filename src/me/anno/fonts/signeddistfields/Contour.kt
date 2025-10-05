package me.anno.fonts.signeddistfields

import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contours.Companion.emptyContours
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.utils.InternalAPI
import org.joml.AABBf

class Contour(val segments: List<EdgeSegment>, val z: Float, val color: Int) {

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
        var calculateContoursImpl: ((Font, CharSequence) -> Contours)? = null
        fun calculateContours(font: Font, text: CharSequence): Contours {
            return calculateContoursImpl?.invoke(font, text) ?: emptyContours
        }
    }
}