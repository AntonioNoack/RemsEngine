package me.anno.graph.visual.vector

import me.anno.graph.visual.FlowGraphNodeUtils.getDoubleInput
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.FlowGraphNodeUtils.getIntInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
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

private val data = LazyMap { type: String ->
    val iType = getVectorType(type)
    val numComp = type[6] - '0'
    TypedNodeData(
        "Combine $type", when (type) {
            "Vector2f", "Vector2d" -> "vec2"
            "Vector3f", "Vector3d" -> "vec3"
            "Vector4f", "Vector4d" -> "vec4"
            "Vector2i" -> "ivec2"
            "Vector3i" -> "ivec3"
            "Vector4i" -> "ivec4"
            else -> throw NotImplementedError()
        } to null,
        listOf(iType, "X", iType, "Y", iType, "Z", iType, "W").subList(0, numComp * 2),
        listOf(type, "Result")
    )
}

class CombineVectorNode : TypedNode(data, vectorTypes), GLSLFuncNode {
    override fun compute() {
        val v = when (outputs[0].type) {
            "Vector2f" -> Vector2f(getFloatInput(0), getFloatInput(1))
            "Vector3f" -> Vector3f(getFloatInput(0), getFloatInput(1), getFloatInput(2))
            "Vector4f" -> Vector4f(getFloatInput(0), getFloatInput(1), getFloatInput(2), getFloatInput(3))
            "Vector2d" -> Vector2d(getDoubleInput(0), getDoubleInput(1))
            "Vector3d" -> Vector3d(getDoubleInput(0), getDoubleInput(1), getDoubleInput(2))
            "Vector4d" -> Vector4d(getDoubleInput(0), getDoubleInput(1), getDoubleInput(2), getDoubleInput(3))
            "Vector2i" -> Vector2i(getIntInput(0), getIntInput(1))
            "Vector3i" -> Vector3i(getIntInput(0), getIntInput(1), getIntInput(2))
            "Vector4i" -> Vector4i(getIntInput(0), getIntInput(1), getIntInput(2), getIntInput(3))
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }
}
