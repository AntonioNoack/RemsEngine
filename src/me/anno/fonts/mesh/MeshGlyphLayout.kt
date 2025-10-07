package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.fonts.Codepoints
import me.anno.fonts.Font
import me.anno.fonts.GlyphLayout
import me.anno.fonts.IEmojiCache
import org.joml.Matrix4x3f
import kotlin.math.min

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class MeshGlyphLayout(
    font: Font, text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int,
    drawCharByChar: Boolean?
) : GlyphLayout(font, text, relativeWidthLimit, maxNumLines) {

    init {
        // ensure meshes exist for all characters
        val meshCache = meshCache
        synchronized(meshCache) {
            for (i in indices) {
                val codepoint = getCodepoint(i)
                meshCache.getOrPut(codepoint) {
                    if (Codepoints.isEmoji(codepoint)) {
                        val emojiId = Codepoints.getEmojiId(codepoint)
                        IEmojiCache.emojiCache.getEmojiMesh(emojiId).waitFor()!!
                    } else { // normal character
                        TextMesh(font, codepoint).mesh
                    }
                }
            }
        }
    }

    var joinedMesh: Mesh? = null

    /**
     * better for the performance of long texts
     * */
    fun createJoinedMesh(dst: Mesh) {
        val meshCache = meshCache
        val hasColors = indices.any { glyphIndex ->
            meshCache[getCodepoint(glyphIndex)]?.color0 != null
        }

        this.joinedMesh = object : MeshJoiner<Int>(hasColors, false, false) {
            override fun getMesh(element: Int): Mesh {
                val codepoint = getCodepoint(element)
                return meshCache[codepoint]!!
            }

            override fun getTransform(element: Int, dst: Matrix4x3f) {
                val codepoint = getCodepoint(element)
                val px = getX0(element) * baseScale
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

    fun getOrCreateMesh(): Mesh {
        if (joinedMesh == null) createJoinedMesh(Mesh())
        return joinedMesh!!
    }

    // very long strings just are displayed char by char
    private val drawCharByChar = drawCharByChar ?: (size !in 5..512)

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawMeshCallback) {
        if (isEmpty()) return
        if (this@MeshGlyphLayout.drawCharByChar || startIndex > 0 || endIndex < size) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            drawBuffer.draw(getOrCreateMesh(), 0f, 0f, 0f, width)
        }
    }

    private fun drawSlowly(startIndex: Int, endIndex: Int, callback: DrawMeshCallback) {
        val meshes = meshCache
        for (index in startIndex until min(endIndex, size)) {
            val codepoint = getCodepoint(index)
            val x0 = getX0(index) * baseScale
            val x1 = getX1(index) * baseScale
            val y = getY(index) * baseScale
            val lineWidth = getLineWidth(index) * baseScale
            if (callback.draw(meshes[codepoint]!!, x0, x1, y, lineWidth)) break
        }
    }

    override fun destroy() {
        joinedMesh?.destroy()
        joinedMesh = null
    }
}