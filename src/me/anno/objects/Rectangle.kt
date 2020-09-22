package me.anno.objects

import me.anno.objects.geometric.Polygon

object Rectangle {
    fun create(): Polygon {
        val quad = Polygon(null)
        quad.name = "Rectangle"
        quad.vertexCount.set(4)
        quad.autoAlign = true
        return quad
    }
}