package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.FloatMathsBinary
import me.anno.graph.visual.scalar.FloatMathsTernary
import me.anno.graph.visual.scalar.FloatMathsUnary
import me.anno.graph.visual.scalar.MathNodeData
import me.anno.graph.visual.scalar.TypedMathNode
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

private val types = "Vector2f,Vector3f,Vector4f".split(',')

private val dataF1X = LazyMap { type: String ->
    MathNodeData(
        FloatMathsUnary.supportedUnaryVecTypes,
        listOf(type), type,
        FloatMathsUnary::id, FloatMathsUnary::glsl
    )
}

class MathF1XNode : TypedMathNode<FloatMathsUnary>(dataF1X, types) {
    override fun compute() {
        val v: Any = when (val a = getInput(0)) {
            is Vector2f -> Vector2f(type.float(a.x), type.float(a.y))
            is Vector3f -> Vector3f(type.float(a.x), type.float(a.y), type.float(a.z))
            is Vector4f -> Vector4f(type.float(a.x), type.float(a.y), type.float(a.z), type.float(a.w))
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }
}

private val dataF22 = LazyMap { type: String ->
    MathNodeData(
        FloatMathsBinary.entries.filter { it.supportsVectors },
        listOf(type, type), type,
        FloatMathsBinary::id, FloatMathsBinary::glsl
    )
}

class MathF2XNode : TypedMathNode<FloatMathsBinary>(dataF22, types) {
    override fun compute() {
        val b = getInput(1)
        val v: Any = when (val a = getInput(0)) {
            is Vector2f -> {
                b as Vector2f
                Vector2f(
                    type.float(a.x, b.x),
                    type.float(a.y, b.y)
                )
            }
            is Vector3f -> {
                b as Vector3f
                Vector3f(
                    type.float(a.x, b.x),
                    type.float(a.y, b.y),
                    type.float(a.z, b.z)
                )
            }
            is Vector4f -> {
                b as Vector4f
                Vector4f(
                    type.float(a.x, b.x),
                    type.float(a.y, b.y),
                    type.float(a.z, b.z),
                    type.float(a.w, b.w)
                )
            }
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }
}

private val dataF3X = LazyMap { type: String ->
    MathNodeData(
        FloatMathsTernary.entries,
        listOf(type, type, type), type,
        FloatMathsTernary::id, FloatMathsTernary::glsl
    )
}

class MathF3XNode : TypedMathNode<FloatMathsTernary>(dataF3X, types) {
    override fun compute() {
        val b = getInput(1)
        val c = getInput(2)
        val v: Any = when (val a = getInput(0)) {
            is Vector2f -> {
                b as Vector2f
                c as Vector2f
                Vector2f(
                    type.float(a.x, b.x, c.x),
                    type.float(a.y, b.y, c.y)
                )
            }
            is Vector3f -> {
                b as Vector3f
                c as Vector3f
                Vector3f(
                    type.float(a.x, b.x, c.x),
                    type.float(a.y, b.y, c.y),
                    type.float(a.z, b.z, c.z)
                )
            }
            is Vector4f -> {
                b as Vector4f
                c as Vector4f
                Vector4f(
                    type.float(a.x, b.x, c.x),
                    type.float(a.y, b.y, c.y),
                    type.float(a.z, b.z, c.z),
                    type.float(a.w, b.w, c.w)
                )
            }
            else -> throw NotImplementedError()
        }
        setOutput(0, v)
    }
}