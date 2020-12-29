package me.anno.fonts.mesh

import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.buffer.StaticBuffer
import java.awt.Font

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextMeshGroup(
    font: Font, text: String,
    charSpacing: Float,
    forceVariableBuffer: Boolean,
    debugPieces: Boolean = false
) : TextGroup(
    font, text, charSpacing
) {

    init {
        // ensure triangle buffers for all characters
        val buffers = alignment.buffers
        synchronized(buffers) {
            codepoints.toSet().forEach { char ->
                var buffer = buffers[char]
                if (buffer == null) {
                    buffer = TextMesh(font, String(Character.toChars(char)), debugPieces).buffer
                    buffers[char] = buffer
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
        var vertexCount = 0
        codepoints.forEach { codepoint ->
            vertexCount += characters[codepoint]!!.vertexCount
        }
        val buffer = StaticBuffer(b0.attributes, vertexCount)
        val components = b0.attributes.sumBy { it.components }
        codepoints.forEachIndexed { index, codePoint ->
            val offset = offsets[index] * baseScale
            val subBuffer = characters[codePoint]!!
            val fb = subBuffer.nioBuffer!!
            var k = 0
            for (i in 0 until subBuffer.vertexCount) {
                buffer.put((fb.getFloat(4 * k++) + offset).toFloat())
                for (j in 1 until components) {
                    buffer.put(fb.getFloat(4 * k++))
                }
            }
        }
        this.buffer = buffer
    }

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    private val isSmallBuffer = forceVariableBuffer || codepoints.size < 5 || codepoints.size > 512

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        if (codepoints.isEmpty()) return
        if (isSmallBuffer || startIndex > 0 || endIndex < codepoints.size) {
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
        for (index in startIndex until endIndex) {
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            drawBuffer(characters[codePoint]!!, null, offset)
        }
    }

    override fun destroy() {
        buffer?.destroy()
        buffer = null
    }

    companion object {
        val alignments = HashMap<Font, AlignmentGroup>()
        fun getAlignments(font: Font): AlignmentGroup {
            var alignment = alignments[font]
            if (alignment != null) return alignment
            alignment = AlignmentGroup(HashMap(), HashMap(), HashMap())
            alignments[font] = alignment
            return alignment
        }
    }

}