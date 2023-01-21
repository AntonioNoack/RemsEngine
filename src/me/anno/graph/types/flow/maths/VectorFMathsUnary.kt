package me.anno.graph.types.flow.maths

import org.joml.Vector3d
import org.joml.Vector3f

enum class VectorFMathsUnary(
    val id: Int, val glsl: String,
    val float: (a: Vector3f) -> Float,
    val double: (a: Vector3d) -> Double
) {

    LENGTH(100, "length(a)", { it.length() }, { it.length() }),
    LENGTH_SQUARED(101, "dot(a,a)", { it.lengthSquared() }, { it.lengthSquared() }),
    ;

    companion object {
        val values = values()
        val byId = values.associateBy { it.id }
    }

}