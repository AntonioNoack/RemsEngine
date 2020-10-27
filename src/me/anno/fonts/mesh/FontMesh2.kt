package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.accumulate
import me.anno.utils.joinChars
import org.joml.Matrix4fArrayList
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.lang.RuntimeException
import kotlin.streams.toList

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class FontMesh2(val font: Font, val text: String, val charSpacing: Float,
                forceVariableBuffer: Boolean,
                debugPieces: Boolean = false) : FontMeshBase() {

    val alignment = getAlignments(font)
    val ctx = FontRenderContext(null, true, true)

    val codepoints = text.codePoints().toList()

    val offsets = (codepoints.mapIndexed { index, secondCodePoint ->
        if (index > 0) {
            val firstCodePoint = codepoints[index - 1]
            charSpacing + getOffset(firstCodePoint, secondCodePoint)
        } else 0.0
    } + getOffset(codepoints.last(), 32)).accumulate() // space

    val baseScale: Float

    init {
        if('\t' in text || '\n' in text) throw RuntimeException("\t and \n are not allowed in FontMesh2!")
        val layout = TextLayout(".", font, ctx)
        baseScale = FontMesh.DEFAULT_LINE_HEIGHT / (layout.ascent + layout.descent)
        minX = 0f
        maxX = 0f
    }

    fun getOffset(previous: Int, current: Int): Double {
        val map = alignment.first
        val key = previous to current
        val characterLengthCache = alignment.second
        fun getLength(str: String): Double {
            return TextLayout(str, font, ctx).bounds.maxX
        }
        fun getCharLength(char: Int): Double {
            var value = characterLengthCache[char]
            if (value != null) return value
            value = getLength(String(Character.toChars(char)))
            characterLengthCache[char] = value
            return value
        }
        synchronized(alignment) {
            var offset = map[key]
            if (offset != null) return offset
            // val aLength = getCharLength(previous)
            val bLength = getCharLength(current)
            val abLength = getLength(String(Character.toChars(previous) + Character.toChars(current)))
            // ("$abLength = $aLength + $bLength ? (${aLength + bLength})")
            offset = abLength - bLength
            //(abLength - (bLength + aLength) * 0.5)
            // offset = (abLength - aLength)
            // offset = aLength
            map[key] = offset
            return offset
        }
    }

    init {
        // ensure triangle buffers for all characters
        val buffers = alignment.third
        synchronized(buffers) {
            codepoints.toSet().forEach { char ->
                var buffer = buffers[char]
                if (buffer == null) {
                    buffer = FontMesh(font, String(Character.toChars(char)), debugPieces).buffer
                    buffers[char] = buffer
                }
            }
        }
    }

    var buffer: StaticBuffer? = null

    // better for the performance of long texts
    fun createStaticBuffer(){
        // ("creating large ${codepoints.joinChars()}")
        val characters = alignment.third
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
            for(i in 0 until subBuffer.vertexCount){
                buffer.put((fb.getFloat(4 * k++) + offset).toFloat())
                for(j in 1 until components){
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
    override fun draw(drawBuffer: (StaticBuffer, offset: Float) -> Unit) {
        if(codepoints.isEmpty()) return
        if(isSmallBuffer){
            drawSlowly(drawBuffer)
        } else {
            if(buffer == null) createStaticBuffer()
            drawBuffer(buffer!!, 0f)
        }
    }

    fun drawSlowly(drawBuffer: (StaticBuffer, offset: Float) -> Unit){
        val characters = alignment.third
        codepoints.forEachIndexed { index, codePoint ->
            val offset = (offsets[index] * baseScale).toFloat()
            drawBuffer(characters[codePoint]!!, offset)
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