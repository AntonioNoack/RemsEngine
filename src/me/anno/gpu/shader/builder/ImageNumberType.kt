package me.anno.gpu.shader.builder

enum class ImageNumberType(val id: Int) {
    FLOAT(Variable.NUMBER_FLOAT),
    INT(Variable.NUMBER_INT),
    UINT(Variable.NUMBER_UINT)
}