package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.fonts.Codepoints
import me.anno.fonts.Font
import me.anno.fonts.GlyphLayout
import org.joml.Matrix4x3f
import kotlin.math.max
import kotlin.math.min

/**
 * Converts a font and text into a mesh(list) for each glyph
 * */
class MeshGlyphLayout(
    font: Font, text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int
) : GlyphLayout(font, text, relativeWidthLimit, maxNumLines) {

    private val meshCache = GlyphMeshCache.getMeshCache(font)

    /**
     * better for the performance of long texts
     * todo use relativeWidthLimit as right side for line alignment?
     * @param lineAlignmentX [0,1], 0 = left, 0.5 = middle, 1.0 = right
     * */
    fun createJoinedMesh(dst: Mesh, lineAlignmentX: Float): Mesh {
        val meshCache = meshCache
        val hasColors = indices.any { glyphIndex ->
            val codepoint = getCodepoint(glyphIndex)
            GlyphMeshCache.getMesh(meshCache, font, codepoint).color0 != null
        }

        return object : MeshJoiner<Int>(hasColors, false, false) {
            override fun getMesh(element: Int): Mesh {
                val codepoint = getCodepoint(element)
                return GlyphMeshCache.getMesh(meshCache, font, codepoint)
            }

            override fun getTransform(element: Int, dst: Matrix4x3f) {
                val codepoint = getCodepoint(element)
                val alignmentOffset = (width - getLineWidth(element)) * lineAlignmentX
                val px = (getX0(element) + alignmentOffset) * baseScale
                val py = getY(element) * baseScale
                dst.translation(px, -py, 0f)

                if (Codepoints.isEmoji(codepoint)) {
                    // emojiMesh, we need more than just translation
                    dst.scale(0.5f)
                    dst.translate(1.2f, 0.6f, 0f)
                }
            }
        }.join(dst, indices.toList())
    }

    fun draw(startIndex: Int, endIndex: Int, callback: DrawMeshCallback) {
        for (glyphIndex in max(startIndex, 0) until min(endIndex, size)) {
            val codepoint = getCodepoint(glyphIndex)
            val x0 = getX0(glyphIndex) * baseScale
            val x1 = getX1(glyphIndex) * baseScale
            val y = getY(glyphIndex) * baseScale
            val lineWidth = getLineWidth(glyphIndex) * baseScale
            val mesh = GlyphMeshCache.getMesh(meshCache, font, codepoint)
            if (callback.draw(mesh, x0, x1, y, lineWidth, glyphIndex)) break
        }
    }
}