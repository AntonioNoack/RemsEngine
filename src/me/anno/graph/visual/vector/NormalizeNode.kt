package me.anno.graph.visual.vector

import me.anno.graph.visual.FlowGraphNodeUtils.getDoubleInput
import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

class NormalizeNode : TypedNode(data, vectorTypes) {
    init {
        setInput(1, 1.0)
    }

    override fun compute() {
        val length = getDoubleInput(1)
        val v: Any = when (val a = getInput(0)) {
            is Vector2f -> a.normalize(length.toFloat())
            is Vector3f -> a.normalize(length.toFloat())
            is Vector4f -> a.normalize(length.toFloat())
            is Vector2d -> a.normalize(length)
            is Vector3d -> a.normalize(length)
            is Vector4d -> a.normalize(length)
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }

    companion object {
        private val data = LazyMap { type: String ->
            TypedNodeData(
                "$type Normalize",
                "norm$type" to "normalize(a)*b",
                listOf(type, "Vector", if (type.last() == 'f') "Float" else "Double", "Length"),
                listOf(type, "Result")
            )
        }
    }
}
