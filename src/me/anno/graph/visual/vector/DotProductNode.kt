package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

@Suppress("unused")
class DotProductNode : TypedNode(data, vectorTypes) {
    override fun compute() {
        val b = getInput(1)
        val v = when (val a = getInput(0)) {
            is Vector2f -> a.dot(b as Vector2f)
            is Vector3f -> a.dot(b as Vector3f)
            is Vector4f -> a.dot(b as Vector4f)
            is Vector2d -> a.dot(b as Vector2d)
            is Vector3d -> a.dot(b as Vector3d)
            is Vector4d -> a.dot(b as Vector4d)
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }

    companion object {
        private val data = LazyMap { type: String ->
            TypedNodeData(
                "$type Dot", "dot" to null, listOf(type, "A", type, "B"),
                listOf(if (type.last() == 'f') "Float" else "Double", "Result")
            )
        }
    }
}
