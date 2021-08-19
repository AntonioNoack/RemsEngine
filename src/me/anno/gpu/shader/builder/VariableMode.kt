package me.anno.gpu.shader.builder

enum class VariableMode(val glslName: String) {
    IN("in"),
    OUT("out"),
    INOUT("inout")
}