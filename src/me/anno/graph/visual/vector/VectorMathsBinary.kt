package me.anno.graph.visual.vector

import org.joml.Vector3d
import org.joml.Vector3f

enum class VectorMathsBinary(
    val id: Int,
    val glsl: String,
    val vector3f: (a: Vector3f, b: Vector3f, d: Vector3f) -> Vector3f,
    val vector3d: (a: Vector3d, b: Vector3d, d: Vector3d) -> Vector3d,
) {

    CROSS(70, "cross(a,b)", { a, b, d -> a.cross(b, d) }, { a, b, d -> a.cross(b, d) }),

    ;

}