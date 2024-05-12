package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

class CrossProductNode : TypedNode(crossData, types) {
    override fun compute() {
        val b = getInput(1)
        val v: Any = when (val a = getInput(0)) {
            is Vector2f -> a.cross(b as Vector2f)
            is Vector3f -> a.cross(b as Vector3f)
            is Vector2d -> a.cross(b as Vector2d)
            is Vector3d -> a.cross(b as Vector3d)
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }

    companion object {
        private val types = "Vector2f,Vector3f,Vector2d,Vector3d".split(',')
        private val crossData = LazyMap { type: String ->
            TypedNodeData(
                "$type Cross",
                if (type[6] == '2') "cross$type" to "a.x*b.y-a.y*b.x" else "cross" to null,
                listOf(type, "A", type, "B"),
                listOf(getVectorTypeF(type), "Result")
            )
        }
    }
}
