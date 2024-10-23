package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.FloatMathBinary
import me.anno.graph.visual.scalar.FloatMathTernary
import me.anno.graph.visual.scalar.FloatMathUnary
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

private val dataF1X = LazyMap { type: String ->
    MathNodeData(
        FloatMathUnary.supportedUnaryVecTypes,
        listOf(type), type,
        FloatMathUnary::id, { mode ->
            if (mode == FloatMathUnary.TRUNCATE) {
                when (type) {
                    "Vector2f", "Vector2d" -> "vec2(ivec2(a))"
                    "Vector3f", "Vector3d" -> "vec3(ivec3(a))"
                    "Vector4f", "Vector4d" -> "vec4(ivec4(a))"
                    "Vector2i", "Vector3i", "Vector4i" -> "a" // already is truncated -> nothing left to do
                    else -> assertFail("Unsupported Type")
                }
            } else mode.glsl
        }
    )
}

class MathF1XNode : TypedMathNode<FloatMathUnary>(dataF1X, vectorTypes) {
    override fun compute() {
        val t = enumType
        val v = when (val a = getInput(0)) {
            is Vector2f -> Vector2f(t.f32(a.x), t.f32(a.y))
            is Vector3f -> Vector3f(t.f32(a.x), t.f32(a.y), t.f32(a.z))
            is Vector4f -> Vector4f(t.f32(a.x), t.f32(a.y), t.f32(a.z), t.f32(a.w))
            is Vector2d -> Vector2d(t.f64(a.x), t.f64(a.y))
            is Vector3d -> Vector3d(t.f64(a.x), t.f64(a.y), t.f64(a.z))
            is Vector4d -> Vector4d(t.f64(a.x), t.f64(a.y), t.f64(a.z), t.f64(a.w))
            is Vector2i -> Vector2i(t.i32(a.x), t.i32(a.y))
            is Vector3i -> Vector3i(t.i32(a.x), t.i32(a.y), t.i32(a.z))
            is Vector4i -> Vector4i(t.i32(a.x), t.i32(a.y), t.i32(a.z), t.i32(a.w))
            else -> assertFail("Unsupported Type")
        }
        setOutput(0, v)
    }
}

private val dataF22 = LazyMap { type: String ->
    MathNodeData(
        FloatMathBinary.entries.filter { it.supportsVectors },
        listOf(type, type), type,
        FloatMathBinary::id, FloatMathBinary::glsl
    )
}

class MathF2XNode : TypedMathNode<FloatMathBinary>(dataF22, vectorTypes) {
    override fun compute() {
        val t = enumType
        val b = getInput(1)
        val v = when (val a = getInput(0)) {
            is Vector2f -> Vector2f(t.f32(a.x, (b as Vector2f).x), t.f32(a.y, b.y))
            is Vector3f -> Vector3f(t.f32(a.x, (b as Vector3f).x), t.f32(a.y, b.y), t.f32(a.z, b.z))
            is Vector4f -> Vector4f(t.f32(a.x, (b as Vector4f).x), t.f32(a.y, b.y), t.f32(a.z, b.z), t.f32(a.w, b.w))
            is Vector2d -> Vector2d(t.f64(a.x, (b as Vector2d).x), t.f64(a.y, b.y))
            is Vector3d -> Vector3d(t.f64(a.x, (b as Vector3d).x), t.f64(a.y, b.y), t.f64(a.z, b.z))
            is Vector4d -> Vector4d(t.f64(a.x, (b as Vector4d).x), t.f64(a.y, b.y), t.f64(a.z, b.z), t.f64(a.w, b.w))
            is Vector2i -> Vector2i(t.i32(a.x, (b as Vector2i).x), t.i32(a.y, b.y))
            is Vector3i -> Vector3i(t.i32(a.x, (b as Vector3i).x), t.i32(a.y, b.y), t.i32(a.z, b.z))
            is Vector4i -> Vector4i(t.i32(a.x, (b as Vector4i).x), t.i32(a.y, b.y), t.i32(a.z, b.z), t.i32(a.w, b.w))
            else -> assertFail("Unsupported Type")
        }
        setOutput(0, v)
    }
}

private val dataF3X = LazyMap { type: String ->
    MathNodeData(
        FloatMathTernary.entries,
        listOf(type, type, type), type,
        FloatMathTernary::id, FloatMathTernary::glsl
    )
}

class MathF3XNode : TypedMathNode<FloatMathTernary>(dataF3X, vectorTypes) {
    override fun compute() {
        val t = enumType
        val b = getInput(1)
        val c = getInput(2)
        val v = when (val a = getInput(0)) {
            is Vector2f -> Vector2f(t.f32(a.x, (b as Vector2f).x, (c as Vector2f).x), t.f32(a.y, b.y, c.y))
            is Vector3f -> Vector3f(
                t.f32(a.x, (b as Vector3f).x, (c as Vector3f).x),
                t.f32(a.y, b.y, c.y), t.f32(a.z, b.z, c.z)
            )
            is Vector4f -> Vector4f(
                t.f32(a.x, (b as Vector4f).x, (c as Vector4f).x),
                t.f32(a.y, b.y, c.y), t.f32(a.z, b.z, c.z), t.f32(a.w, b.w, c.w)
            )
            is Vector2d -> Vector2d(t.f64(a.x, (b as Vector2d).x, (c as Vector2d).x), t.f64(a.y, b.y, c.y))
            is Vector3d -> Vector3d(
                t.f64(a.x, (b as Vector3d).x, (c as Vector3d).x),
                t.f64(a.y, b.y, c.y), t.f64(a.z, b.z, c.z)
            )
            is Vector4d -> Vector4d(
                t.f64(a.x, (b as Vector4d).x, (c as Vector4d).x),
                t.f64(a.y, b.y, c.y), t.f64(a.z, b.z, c.z), t.f64(a.w, b.w, c.w)
            )
            is Vector2i -> Vector2i(t.i32(a.x, (b as Vector2i).x, (c as Vector2i).x), t.i32(a.y, b.y, c.y))
            is Vector3i -> Vector3i(
                t.i32(a.x, (b as Vector3i).x, (c as Vector3i).x),
                t.i32(a.y, b.y, c.y), t.i32(a.z, b.z, c.z)
            )
            is Vector4i -> Vector4i(
                t.i32(a.x, (b as Vector4i).x, (c as Vector4i).x),
                t.i32(a.y, b.y, c.y), t.i32(a.z, b.z, c.z), t.i32(a.w, b.w, c.w)
            )
            else -> assertFail("Unsupported Type")
        }
        setOutput(0, v)
    }
}