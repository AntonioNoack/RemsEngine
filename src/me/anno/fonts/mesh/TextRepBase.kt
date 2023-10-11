package me.anno.fonts.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.TextSDF

abstract class TextRepBase : ICacheData {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    fun interface DrawBufferCallback {
        fun draw(mesh: Mesh?, textSDF: TextSDF?, offset: Float)
    }

    abstract fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback)
}