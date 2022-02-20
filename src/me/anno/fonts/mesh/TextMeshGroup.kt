package me.anno.fonts.mesh

import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.buffer.StaticBuffer
import java.awt.Font
import kotlin.math.min

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextMeshGroup(
    font: Font,
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
                    buffers[char] = TextMesh(font, String(Character.toChars(char)), debugPieces).buffer
                }
            }
        }
    }

    var buffer: StaticBuffer? = null

    // better for the performance of long texts
    fun createStaticBuffer() {
        // ("creating large ${codepoints.joinChars()}")
        val characters = alignment.buffers
        val b0 = characters[codepoints.first()]!!
        val vertexCount = codepoints.sumOf { characters[it]!!.vertexCount }
        val dst = StaticBuffer(b0.attributes, vertexCount)
        val components = b0.attributes.sumOf { it.components }
        for (index in codepoints.indices) {
            val codepoint = codepoints[index]
            val offset = offsets[index] * baseScale
            val src = characters[codepoint]!!
            val fb = src.nioBuffer!!
            var k = 0
            for (i in 0 until src.vertexCount) {
                dst.put((fb.getFloat(4 * k++) + offset).toFloat()) // x
                for (j in 1 until components) {// y, z
                    dst.put(fb.getFloat(4 * k++))
                }
            }
        }
        this.buffer = dst
    }

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    private val drawCharByChar = forceVariableBuffer || codepoints.size < 5 || codepoints.size > 512

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        if (codepoints.isEmpty()) return
        if (drawCharByChar || startIndex > 0 || endIndex < codepoints.size) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            if (buffer == null) createStaticBuffer()
            drawBuffer(buffer!!, null, 0f)
        }
    }

    private fun drawSlowly(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        val characters = alignment.buffers
        for (index in startIndex until min(endIndex, codepoints.size)) {
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            drawBuffer(characters[codePoint]!!, null, offset)
        }
    }

    override fun destroy() {
        buffer?.destroy()
        buffer = null
    }

}