package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.maps.KeyPairMap

class AlignmentGroup(
    val charDistance: KeyPairMap<Int, Int, Double>, // |a| = |ab| - |b|
    val charSize: HashMap<Int, Double>,// |a|
    val buffers: HashMap<Int, StaticBuffer> // triangles of a
)
