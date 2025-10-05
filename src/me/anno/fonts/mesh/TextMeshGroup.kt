package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.fonts.Font
import me.anno.fonts.TextGroup
import me.anno.utils.types.Strings.joinChars
import org.joml.Matrix4x3f
import kotlin.math.min

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextMeshGroup(
    font: Font, text: CharSequence,
    charSpacing: Float, forceDrawCharByChar: Boolean
) : TextGroup(font, text, charSpacing.toDouble()) {

    init {
        // ensure meshes exist for all characters
        val meshCache = meshCache
        synchronized(meshCache) {
            for (char in codepoints) {
                meshCache.getOrPut(char) {
                    TextMesh(font, char.joinChars().toString()).mesh
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
        val hasColors = codepoints.any { codepoint -> meshCache[codepoint]!!.color0 != null }
        this.joinedMesh = object : MeshJoiner<Int>(hasColors, false, false) {
            override fun getMesh(element: Int): Mesh {
                val codepoint = codepoints[element]
                return meshCache[codepoint]!!
            }

            override fun getTransform(element: Int, dst: Matrix4x3f) {
                val offset = (offsets[element] * baseScale).toFloat()
                dst.translation(offset, 0f, 0f)
            }
        }.join(dst, codepoints.indices.toList())
    }

    fun getOrCreateMesh(): Mesh {
        if (joinedMesh == null) createJoinedMesh(Mesh())
        return joinedMesh!!
    }

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    private val drawCharByChar = forceDrawCharByChar || codepoints.size < 5 || codepoints.size > 512

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
        val buffers = meshCache
        for (index in startIndex until min(endIndex, codepoints.size)) {
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            callback.draw(buffers[codePoint]!!, null, offset)
        }
    }

    override fun destroy() {
        joinedMesh?.destroy()
        joinedMesh = null
    }
}