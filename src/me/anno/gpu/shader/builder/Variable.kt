package me.anno.gpu.shader.builder

import kotlin.math.max

class Variable(
    val type: String,
    var name: String,
    var arraySize: Int,
    var inOutMode: VariableMode
) {

    constructor(type: String, name: String, inOutMode: VariableMode) :
            this(type, name, -1, inOutMode)

    constructor(type: String, name: String, arraySize: Int, isIn: Boolean) :
            this(type, name, arraySize, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: String, name: String, isIn: Boolean) :
            this(type, name, -1, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: String, name: String, arraySize: Int) :
            this(type, name, arraySize, VariableMode.IN)

    constructor(type: String, name: String) :
            this(type, name, -1, VariableMode.IN)

    fun flat(): Variable {
        isFlat = true
        return this
    }

    val size = when (type) {
        "float", "int", "bool" -> 1
        "vec2", "ivec2" -> 2
        "vec3", "ivec3" -> 3
        "vec4", "ivec4" -> 4
        "mat3" -> 9
        "mat4" -> 16
        "mat4x3" -> 12
        else -> 100
    } * max(1, arraySize)

    fun appendGlsl(code: StringBuilder, prefix: String) {
        code.append(prefix)
        code.append(type)
        code.append(' ')
        code.append(name)
        if (arraySize > 0) {
            code.append('[')
            code.append(arraySize)
            code.append(']')
        }
        code.append(";\n")
    }

    var isFlat = false

    override fun equals(other: Any?): Boolean {
        return other is Variable && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "${inOutMode.glslName} $type $name"
    }

    val isInput get() = inOutMode != VariableMode.OUT
    val isOutput get() = inOutMode != VariableMode.IN

}
