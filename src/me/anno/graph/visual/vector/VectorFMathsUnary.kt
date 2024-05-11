package me.anno.graph.visual.vector

import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

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