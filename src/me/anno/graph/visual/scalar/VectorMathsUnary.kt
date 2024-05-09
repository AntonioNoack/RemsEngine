package me.anno.graph.visual.scalar

import org.joml.Vector3d
import org.joml.Vector3f

enum class VectorMathsUnary(
    val id: Int, val glsl: String,
    val float: (a: Vector3f, r: Vector3f) -> Vector3f,
    val double: (a: Vector3d, r: Vector3d) -> Vector3d
) {

    NORMALIZE(70, "normalize(a)", { a, r -> a.normalize(r) }, { a, r -> a.normalize(r) }),
    ;

}