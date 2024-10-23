package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.MathNodeData
import me.anno.graph.visual.scalar.TypedMathNode
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
import kotlin.math.abs

private val vectorLengthData = LazyMap { type: String ->
    MathNodeData(
        VectorLengthMode.entries,
        listOf(type), getVectorTypeF(type),
        VectorLengthMode::id, { enumType ->
            if (enumType != VectorLengthMode.NORM1) enumType.glsl1
            else enumType.glsl1.replace('4', type[6])
        },
        listOf("$type Length", "$type Length Squared", "$type Norm1 Length")
    )
}

class VectorLengthNode : TypedMathNode<VectorLengthMode>(vectorLengthData, vectorTypes) {
    override fun compute() {
        val a = getInput(0)
        val v = when (enumType) {
            VectorLengthMode.LENGTH -> when (a) {
                is Vector2f -> a.length()
                is Vector3f -> a.length()
                is Vector4f -> a.length()
                is Vector2d -> a.length()
                is Vector3d -> a.length()
                is Vector4d -> a.length()
                is Vector2i -> a.length()
                is Vector3i -> a.length()
                is Vector4i -> a.length()
                else -> assertFail("Unsupported Type")
            }
            VectorLengthMode.LENGTH_SQUARED -> when (a) {
                is Vector2f -> a.lengthSquared()
                is Vector3f -> a.lengthSquared()
                is Vector4f -> a.lengthSquared()
                is Vector2d -> a.lengthSquared()
                is Vector3d -> a.lengthSquared()
                is Vector4d -> a.lengthSquared()
                is Vector2i -> a.lengthSquared() // not really double... is that an issue?
                is Vector3i -> a.lengthSquared()
                is Vector4i -> a.lengthSquared()
                else -> assertFail("Unsupported Type")
            }
            VectorLengthMode.NORM1 -> when (a) {
                is Vector2f -> abs(a.x) + abs(a.y)
                is Vector3f -> abs(a.x) + abs(a.y) + abs(a.z)
                is Vector4f -> abs(a.x) + abs(a.y) + abs(a.z) + abs(a.w)
                is Vector2d -> abs(a.x) + abs(a.y)
                is Vector3d -> abs(a.x) + abs(a.y) + abs(a.z)
                is Vector4d -> abs(a.x) + abs(a.y) + abs(a.z) + abs(a.w)
                is Vector2i -> abs(a.x).toLong() + abs(a.y) // not really double... is that an issue?
                is Vector3i -> abs(a.x).toLong() + abs(a.y) + abs(a.z)
                is Vector4i -> abs(a.x).toLong() + abs(a.y) + abs(a.z) + abs(a.w)
                else -> assertFail("Unsupported Type")
            }
        }
        setOutput(0, v)
    }
}
