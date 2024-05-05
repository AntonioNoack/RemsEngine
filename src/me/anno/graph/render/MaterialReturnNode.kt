package me.anno.graph.render

import me.anno.graph.types.flow.ReturnNode
import me.anno.utils.types.Strings.upperSnakeCaseToTitle
import org.joml.Vector2f
import org.joml.Vector3f

class MaterialReturnNode : ReturnNode(outputs) {

    init {
        val layers = MaterialGraph.layers
        for (i in layers.indices) {
            val layer = layers[i]
            val v = layer.defaultWorkValue
            val defaultValue: Any = when (layer.workDims) {
                1 -> v.z
                2 -> Vector2f(v.y, v.z)
                3 -> Vector3f(v.x, v.y, v.z)
                else -> v
            }
            val con = inputs[i + 1]
            con.currValue = defaultValue
            con.defaultValue = defaultValue
        }
    }

    companion object {
        val outputs = MaterialGraph.layers.flatMap {
            listOf(
                MaterialGraph.types[it.workDims - 1],
                it.name.upperSnakeCaseToTitle()
            )
        }
    }
}