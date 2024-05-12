package me.anno.graph.visual.vector

enum class VectorLengthMode(val id: Int, val glsl1: String, val glsl2: String) {
    LENGTH(0, "length(a)", "length(a-b)"),
    LENGTH_SQUARED(1, "dot(a,a)", "dot(a-b,a-b)"),
    NORM1(2, "dot(abs(a),vec4(1.0))", "dot(abs(a-b),vec4(1.0))")
}