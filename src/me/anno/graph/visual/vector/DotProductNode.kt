package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i

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
            is Vector2i -> a.dot(b as Vector2i)
            is Vector3i -> a.dot(b as Vector3i)
            is Vector4i -> a.dot(b as Vector4i)
            else -> assertFail("Unsupported Type")
        }
        setOutput(0, v)
    }

    companion object {
        private val data = LazyMap { type: String ->
            val retType = when (type.last()) {
                'f' -> "Float"
                'i' -> "Long"
                'd' -> "Double"
                else -> assertFail("Unknown $type")
            }
            TypedNodeData("$type Dot", "dot" to null, listOf(type, "A", type, "B"), listOf(retType, "Result"))
        }
    }
}
