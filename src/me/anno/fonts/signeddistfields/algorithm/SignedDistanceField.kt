package me.anno.fonts.signeddistfields.algorithm

import me.anno.config.ConfigRef
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.Lists.all2
import org.joml.Vector2f
import java.nio.FloatBuffer

object SignedDistanceField {

    val padding by ConfigRef("rendering.signedDistanceFields.padding", 10f)
    val sdfResolution by ConfigRef("rendering.signedDistanceFields.resolution", 1f)

    private fun getDistanceComputer(font: Font, text: CharSequence, roundEdges: Boolean): SignedDistanceField2? {
        val contours = calculateContours(font, text)
        if (contours.all2 { it.segments.isEmpty() }) {
            return null
        }
        return SignedDistanceField2(contours, roundEdges, sdfResolution, padding)
    }

    fun createBuffer(font: Font, text: String, roundEdges: Boolean): FloatBuffer? {
        return getDistanceComputer(font, text, roundEdges)?.distances
    }

    fun createTexture(font: Font, text: CharSequence, roundEdges: Boolean): TextSDF {
        val stats = getDistanceComputer(font, text, roundEdges)
        val buffer = stats?.distances ?: return TextSDF.empty

        val tex = Texture2D("SDF[$font,'$text',$roundEdges]", stats.w, stats.h, 1)
        addGPUTask("${tex.name}.createTexture()", stats.w, stats.h) {
            tex.createMonochromeFP16(buffer, true)
            tex.ensureFilterAndClamping(Filtering.TRULY_LINEAR, Clamping.CLAMP)
            ByteBufferPool.free(buffer)
        }

        // the center, because we draw the pieces from the center
        val ox = +(stats.maxX + stats.minX) * sdfResolution / stats.w
        val oy = -(stats.maxY + stats.minY) * sdfResolution / stats.h // mirrored for OpenGL
        return TextSDF(tex, Vector2f(ox, oy))
    }
}