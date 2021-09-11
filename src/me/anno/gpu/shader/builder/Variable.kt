package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredSettingsV2.Companion.glslTypes
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

    constructor(components: Int, name: String) :
            this(glslTypes[components - 1], name, VariableMode.IN)

    constructor(components: Int, name: String, inOutMode: VariableMode) :
            this(glslTypes[components - 1], name, inOutMode)

    constructor(components: Int, name: String, arraySize: Int = -1) :
            this(glslTypes[components - 1], name, arraySize)

    constructor(type: String, name: String) :
            this(type, name, -1, VariableMode.IN)

    fun flat(): Variable {
        isFlat = true
        return this
    }

    val size = when (type) {
        "bool" -> 5
        "int" -> 7
        "float" -> 10
        "vec2", "ivec2" -> 20
        "vec3", "ivec3" -> 30
        "vec4", "ivec4" -> 40
        "mat3" -> 90
        "mat4" -> 160
        "mat4x3" -> 120
        else -> 1000
    } * max(1, arraySize)

    fun appendGlsl(code: StringBuilder, prefix: String) {
        if (prefix.startsWith("uniform") && arraySize > 0 && type.startsWith("sampler")) {
            for (index in 0 until arraySize) {
                code.append(prefix)
                code.append(' ')
                code.append(type)
                code.append(' ')
                code.append(name)
                code.append(index)
                code.append(";\n")
            }
        } else {
            code.append(prefix)
            code.append(' ')
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

    val isAttribute get() = inOutMode == VariableMode.ATTR
    val isInput get() = inOutMode != VariableMode.OUT
    val isOutput get() = inOutMode == VariableMode.OUT || inOutMode == VariableMode.INOUT

}
