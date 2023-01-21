package me.anno.graph.types.flow.maths

import org.joml.*

enum class VectorFMathsUnary(
    val id: Int, val glsl: String,
    val v2f: (a: Vector2f) -> Float,
    val v2d: (a: Vector2d) -> Double,
    val v3f: (a: Vector3f) -> Float,
    val v3d: (a: Vector3d) -> Double,
    val v4f: (a: Vector4f) -> Float,
    val v4d: (a: Vector4d) -> Double,
) {

    LENGTH(100, "length(a)", { it.length() }, { it.length() }, { it.length() },
        { it.length() }, { it.length() }, { it.length() }),
    LENGTH_SQUARED(101, "dot(a,a)", { it.lengthSquared() }, { it.lengthSquared() }, { it.lengthSquared() },
        { it.lengthSquared() }, { it.lengthSquared() }, { it.lengthSquared() }),
    ;

}