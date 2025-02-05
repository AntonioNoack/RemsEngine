package me.anno.fonts.signeddistfields.algorithm

import me.anno.config.ConfigRef
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.structures.lists.Lists.all2
import org.joml.Vector2f

object SignedDistanceField {

    val padding by ConfigRef("rendering.signedDistanceFields.padding", 10f)
    val sdfResolution by ConfigRef("rendering.signedDistanceFields.resolution", 1f)

    fun computeDistances(font: Font, text: CharSequence, roundEdges: Boolean): SignedDistanceField2? {
        // val t0 = Time.nanoTime
        val contours = calculateContours(font, text)
        // warmup ~11.3ms (20%), then 0.1ms (<1%)
        // println("Took ${(Time.nanoTime - t0) / 1e6f} ms for contours")
        if (contours.all2 { it.segments.isEmpty() }) return null
        return SignedDistanceField2(contours, roundEdges, sdfResolution, padding)
    }

    fun createBuffer(font: Font, text: String, roundEdges: Boolean): FloatArray? {
        return computeDistances(font, text, roundEdges)?.distances
    }

    fun createTexture(font: Font, text: CharSequence, roundEdges: Boolean): TextSDF {
        val stats = computeDistances(font, text, roundEdges)
        val buffer = stats?.distances ?: return TextSDF.empty

        val tex = Texture2D("SDF[$font,'$text',$roundEdges]", stats.w, stats.h, 1)
        addGPUTask("${tex.name}.createTexture()", stats.w, stats.h) {
            tex.createMonochromeFP16(buffer, true)
            tex.ensureFilterAndClamping(Filtering.TRULY_LINEAR, Clamping.CLAMP)
        }

        // the center, because we draw the pieces from the center
        val ox = +(stats.maxX + stats.minX) * sdfResolution / stats.w
        val oy = -(stats.maxY + stats.minY) * sdfResolution / stats.h // mirrored for OpenGL
        return TextSDF(tex, Vector2f(ox, oy))
    }
}