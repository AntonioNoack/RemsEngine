package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.maps.KeyPairMap
import java.awt.Font

class AlignmentGroup(
    val charDistance: KeyPairMap<Int, Int, Double>, // |a| = |ab| - |b|
    val charSize: HashMap<Int, Double>,// |a|
    val buffers: HashMap<Int, StaticBuffer> // triangles of a
) {
    companion object {
        private val alignments = HashMap<Font, AlignmentGroup>()
        fun getAlignments(font: Font): AlignmentGroup {
            var alignment = alignments[font]
            if (alignment != null) return alignment
            alignment = AlignmentGroup(KeyPairMap(), HashMap(), HashMap())
            alignments[font] = alignment
            return alignment
        }
    }
}
