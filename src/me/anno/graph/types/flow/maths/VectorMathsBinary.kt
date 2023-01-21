package me.anno.graph.types.flow.maths

import org.joml.Vector3d
import org.joml.Vector3f

enum class VectorMathsBinary(
    val id: Int,
    val glsl: String,
    val float: (a: Vector3f, b: Vector3f, d: Vector3f) -> Vector3f,
    val double: (a: Vector3d, b: Vector3d, d: Vector3d) -> Vector3d
) {

    CROSS(70, "cross(a,b)", { a, b, d -> a.cross(b, d) }, { a, b, d -> a.cross(b, d) }),

    ;

    companion object {
        val values = values()
        val byId = values.associateBy { it.id }
    }

}