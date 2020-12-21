package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import java.awt.Font

class AlignmentGroup(
    val charDistance: HashMap<Pair<Int, Int>, Double>, // |a| = |ab| - |b|
    val charSize: HashMap<Int, Double>,// |a|
    val buffers: HashMap<Int, StaticBuffer> // triangles of a
)
