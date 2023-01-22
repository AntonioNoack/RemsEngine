package me.anno.graph.render

import me.anno.graph.types.flow.ReturnNode
import org.joml.Vector2f
import org.joml.Vector3f

class MaterialReturnNode : ReturnNode(
    MaterialGraph.layers.map {
        listOf(MaterialGraph.types[it.dimensions - 1], it.name)
    }.flatten()
) {
    init {
        val inputs = inputs!!
        val layers = MaterialGraph.layers
        for (i in layers.indices) {
            val layer = layers[i]
            val v = layer.defaultValueARGB
            val defaultValue: Any = when (layer.dimensions) {
                1 -> v.z
                2 -> Vector2f(v.y, v.z)
                3 -> Vector3f(v.x, v.y, v.z)
                else -> v
            }
            val con = inputs[i + 1]
            con.value = defaultValue
            con.defaultValue = defaultValue
        }
    }
}