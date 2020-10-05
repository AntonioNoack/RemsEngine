package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.cache.CacheData
import org.joml.Matrix4fArrayList

abstract class FontMeshBase: CacheData {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    abstract fun draw(matrix: Matrix4fArrayList, drawBuffer: (StaticFloatBuffer) -> Unit)
}