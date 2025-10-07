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

    /**
     * This is just a rough approximation
     * */
    fun getSignedArea(): Double = 0.5 * segments.sumOf { it.getCrossSum() }
    fun isCCW(): Boolean = getSignedArea() > 0.0

    companion object {

        @InternalAPI
        var calculateContoursImpl: ((Font, codepoint: Int) -> Contours)? = null
        fun calculateContours(font: Font, codepoint: Int): Contours {
            return calculateContoursImpl?.invoke(font, codepoint) ?: emptyContours
        }
    }
}