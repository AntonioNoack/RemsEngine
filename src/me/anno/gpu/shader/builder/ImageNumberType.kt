package me.anno.gpu.shader.builder

enum class ImageNumberType(val id: Int) {
    FLOAT01(Variable.NUMBER_FLOAT01),
    FLOAT(Variable.NUMBER_FLOAT),
    INT(Variable.NUMBER_INT),
    UINT(Variable.NUMBER_UINT),
}