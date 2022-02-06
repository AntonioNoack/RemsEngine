package me.anno.remsstudio.objects

import me.anno.language.translation.Dict
import me.anno.remsstudio.objects.geometric.Polygon

object Rectangle {
    fun create(): Polygon {
        val quad = Polygon(null)
        quad.name = Dict["Rectangle", "obj.rectangle"]
        quad.vertexCount.set(4)
        quad.autoAlign = true
        return quad
    }
}