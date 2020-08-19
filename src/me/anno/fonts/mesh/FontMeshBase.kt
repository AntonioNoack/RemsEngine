package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.cache.CacheData
import org.joml.Matrix4fArrayList

abstract class FontMeshBase: CacheData {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    abstract fun draw(matrix: Matrix4fArrayList, drawBuffer: (StaticFloatBuffer) -> Unit)
}