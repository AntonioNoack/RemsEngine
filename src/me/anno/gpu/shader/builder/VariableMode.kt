package me.anno.gpu.shader.builder

enum class VariableMode(val glslName: String) {
    IN("in"),
    ATTR("attr"),
    OUT("out"),
    INOUT("inout")
}