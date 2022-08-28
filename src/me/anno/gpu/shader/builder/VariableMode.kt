package me.anno.gpu.shader.builder

enum class VariableMode(val glslName: String) {
    IN("in"),
    INOUT2("inout2"),
    ATTR("attr"),
    OUT("out"),
    INOUT("inout")
}