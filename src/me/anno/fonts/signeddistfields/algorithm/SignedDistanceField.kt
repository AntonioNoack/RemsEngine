package me.anno.fonts.signeddistfields.algorithm

import me.anno.config.ConfigRef
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.types.Strings.joinChars

object SignedDistanceField {

    val padding by ConfigRef("rendering.signedDistanceFields.padding", 10f)
    val sdfResolution by ConfigRef("rendering.signedDistanceFields.resolution", 1f)

    fun computeDistances(font: Font, codepoint: Int, roundEdges: Boolean): SignedDistanceField2? {
        // val t0 = Time.nanoTime
        val contours = calculateContours(font, codepoint).contours
            .filter { it.segments.isNotEmpty() }
        // warmup ~11.3ms (20%), then 0.1ms (<1%)
        // println("Took ${(Time.nanoTime - t0) / 1e6f} ms for contours")
        return computeDistances(contours, roundEdges)
    }

    fun computeDistances(contours: List<Contour>, roundEdges: Boolean): SignedDistanceField2? {
        if (contours.isEmpty()) return null
        val field = SignedDistanceField2(contours, roundEdges, sdfResolution, padding, true)
        field.getDistances()
        return field
    }

    fun createBuffer(font: Font, codepoint: Int, roundEdges: Boolean): FloatArray? {
        return computeDistances(font, codepoint, roundEdges)?.getDistances()
    }

    fun createTexture(font: Font, codepoint: Int, roundEdges: Boolean): TextSDF {
        val contours = calculateContours(font, codepoint).contours
            .filter { it.segments.isNotEmpty() }
        return createTexture(font, codepoint, roundEdges, contours, 0f)
    }

    fun createTexture(font: Font, codepoint: Int, roundEdges: Boolean, contours: List<Contour>, z: Float): TextSDF {
        if (contours.isEmpty()) return TextSDF.empty
        val stats = computeDistances(contours, roundEdges)
        val buffer = stats?.getDistances() ?: return TextSDF.empty

        val tex = Texture2D("SDF[$font,'${codepoint.joinChars()}',$roundEdges]", stats.w, stats.h, 1)
        addGPUTask("${tex.name}.createTexture()", stats.w, stats.h) {
            tex.createMonochromeFP16(buffer, true)
            tex.ensureFilterAndClamping(Filtering.TRULY_LINEAR, Clamping.CLAMP)
        }

        val sampleContour = contours[0]
        return TextSDF(tex, stats.bounds, codepoint, z, sampleContour.color)
    }

    fun createTextures(font: Font, codepoint: Int, roundEdges: Boolean): List<TextSDF> {
        return calculateContours(font, codepoint).contours
            .collectByZRanges().mapIndexed { i, contours ->
                val z = i * 0.001f
                createTexture(font, codepoint, roundEdges, contours, z)
            }
    }

    private fun List<Contour>.collectByZRanges(): List<List<Contour>> {
        val sorted = filter { it.segments.isNotEmpty() }.sortedBy { it.z }
        val prev = sorted.firstOrNull() ?: return emptyList()
        var prevColor: Int = prev.color
        var prevI = 0
        val result = ArrayList<List<Contour>>()
        for (currI in sorted.indices) {
            val currColor = sorted[currI].color
            if (currColor != prevColor) {
                result.add(sorted.subList(prevI, currI))
                prevColor = currColor
                prevI = currI
            }
        }
        result.add(sorted.subList(prevI, sorted.size))
        return result
    }
}