package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import me.anno.cache.CacheData
import me.anno.fonts.signeddistfields.TextSDF

abstract class TextRepBase: CacheData {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    abstract fun draw(drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit)
}