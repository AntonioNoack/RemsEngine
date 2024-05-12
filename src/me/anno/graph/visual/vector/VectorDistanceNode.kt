package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.MathNodeData
import me.anno.graph.visual.scalar.TypedMathNode
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

private val vectorDistanceData = LazyMap { type: String ->
    MathNodeData(
        VectorLengthMode.entries,
        listOf(type, type), getVectorTypeF(type),
        VectorLengthMode::id, { enumType ->
            if (enumType != VectorLengthMode.NORM1) enumType.glsl2
            else enumType.glsl2.replace('4', type[6])
        },
        listOf("$type Distance", "$type Distance Squared", "$type Norm1 Distance")
    )
}

class VectorDistanceNode : TypedMathNode<VectorLengthMode>(vectorDistanceData, vectorTypes) {
    override fun compute() {
        val a = getInput(0)
        val b = getInput(1)
        val v = when (enumType) {
            VectorLengthMode.LENGTH -> when (a) {
                is Vector2f -> a.distance(b as Vector2f)
                is Vector3f -> a.distance(b as Vector3f)
                is Vector4f -> a.distance(b as Vector4f)
                is Vector2d -> a.distance(b as Vector2d)
                is Vector3d -> a.distance(b as Vector3d)
                is Vector4d -> a.distance(b as Vector4d)
                is Vector2i -> a.distance(b as Vector2i)
                is Vector3i -> a.distance(b as Vector3i)
                is Vector4i -> a.distance(b as Vector4i)
                else -> throw NotImplementedError()
            }
            VectorLengthMode.LENGTH_SQUARED -> when (a) {
                is Vector2f -> a.distanceSquared(b as Vector2f)
                is Vector3f -> a.distanceSquared(b as Vector3f)
                is Vector4f -> a.distanceSquared(b as Vector4f)
                is Vector2d -> a.distanceSquared(b as Vector2d)
                is Vector3d -> a.distanceSquared(b as Vector3d)
                is Vector4d -> a.distanceSquared(b as Vector4d)
                is Vector2i -> a.distanceSquared(b as Vector2i) // not really double -> is that an issue?
                is Vector3i -> a.distanceSquared(b as Vector3i)
                is Vector4i -> a.distanceSquared(b as Vector4i)
                else -> throw NotImplementedError()
            }
            VectorLengthMode.NORM1 -> when (a) {
                is Vector2f -> abs(a.x - (b as Vector2f).x) + abs(a.y - b.y)
                is Vector3f -> abs(a.x - (b as Vector3f).x) + abs(a.y - b.y) + abs(a.z - b.z)
                is Vector4f -> abs(a.x - (b as Vector4f).x) + abs(a.y - b.y) + abs(a.z - b.z) + abs(a.w - b.w)
                is Vector2d -> abs(a.x - (b as Vector2d).x) + abs(a.y - b.y)
                is Vector3d -> abs(a.x - (b as Vector3d).x) + abs(a.y - b.y) + abs(a.z - b.z)
                is Vector4d -> abs(a.x - (b as Vector4d).x) + abs(a.y - b.y) + abs(a.z - b.z) + abs(a.w - b.w)
                is Vector2i -> abs(a.x - (b as Vector2i).x).toLong() + abs(a.y - b.y) // not really double -> is that an issue?
                is Vector3i -> abs(a.x - (b as Vector3i).x).toLong() + abs(a.y - b.y) + abs(a.z - b.z)
                is Vector4i -> abs(a.x - (b as Vector4i).x).toLong() + abs(a.y - b.y) + abs(a.z - b.z) + abs(a.w - b.w)
                else -> throw NotImplementedError()
            }
        }
        setOutput(0, v)
    }
}
