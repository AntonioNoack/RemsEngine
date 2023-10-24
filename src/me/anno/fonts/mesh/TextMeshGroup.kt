package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshJoiner
import me.anno.fonts.AWTFont
import me.anno.fonts.TextGroup
import me.anno.utils.types.Strings.joinChars
import org.joml.Matrix4x3f
import kotlin.math.min

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextMeshGroup(
    font: AWTFont,
    text: CharSequence,
    charSpacing: Float,
    forceVariableBuffer: Boolean,
    debugPieces: Boolean = false
) : TextGroup(
    font, text, charSpacing.toDouble()
) {

    init {
        // ensure triangle buffers for all characters
        val buffers = alignment.buffers
        synchronized(buffers) {
            for (char in codepoints) {
                if (buffers[char] == null) {
                    buffers[char] = TextMesh(font, char.joinChars().toString(), debugPieces).buffer
                }
            }
        }
    }

    var mesh: Mesh? = null

    // better for the performance of long texts
    fun createJoinedMesh(dst: Mesh) {
        val characters = alignment.buffers
        this.mesh = object : MeshJoiner<Int>(false, false, false) {
            override fun getMesh(element: Int): Mesh {
                val codepoint = codepoints[element]
                return characters[codepoint]!!
            }

            override fun getTransform(element: Int, dst: Matrix4x3f) {
                val offset = (offsets[element] * baseScale).toFloat()
                dst.translation(offset, 0f, 0f)
            }
        }.join(dst, codepoints.indices.toList())
    }

    fun getOrCreateMesh(): Mesh {
        if (mesh == null) createJoinedMesh(Mesh())
        return mesh!!
    }

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    private val drawCharByChar = forceVariableBuffer || codepoints.size < 5 || codepoints.size > 512

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        if (codepoints.isEmpty()) return
        if (drawCharByChar || startIndex > 0 || endIndex < codepoints.size) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            drawBuffer.draw(getOrCreateMesh(), null, 0f)
        }
    }

    private fun drawSlowly(startIndex: Int, endIndex: Int, callback: DrawBufferCallback) {
        val characters = alignment.buffers
        for (index in startIndex until min(endIndex, codepoints.size)) {
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            callback.draw(characters[codePoint]!!, null, offset)
        }
    }

    override fun destroy() {
        mesh?.destroy()
        mesh = null
    }
}