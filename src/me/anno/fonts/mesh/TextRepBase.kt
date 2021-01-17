package me.anno.fonts.mesh

import me.anno.cache.data.ICacheData
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.buffer.StaticBuffer

abstract class TextRepBase : ICacheData {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    abstract fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    )
}