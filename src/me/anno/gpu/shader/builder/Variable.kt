package me.anno.gpu.shader.builder

import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import kotlin.math.max

class Variable(val type: GLSLType, var name: String, var arraySize: Int, var inOutMode: VariableMode) {

    constructor(type: GLSLType, name: String, inOutMode: VariableMode) :
            this(type, name, -1, inOutMode)

    constructor(type: GLSLType, name: String, arraySize: Int, isIn: Boolean) :
            this(type, name, arraySize, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: GLSLType, name: String, isIn: Boolean) :
            this(type, name, -1, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: GLSLType, name: String, arraySize: Int) :
            this(type, name, arraySize, VariableMode.IN)

    constructor(components: Int, name: String) :
            this(GLSLType.floats[components - 1], name, VariableMode.IN)

    constructor(components: Int, name: String, inOutMode: VariableMode) :
            this(GLSLType.floats[components - 1], name, inOutMode)

    constructor(components: Int, name: String, arraySize: Int = -1) :
            this(GLSLType.floats[components - 1], name, arraySize)

    constructor(type: GLSLType, name: String) :
            this(type, name, -1, VariableMode.IN)

    constructor(base: Variable, mode: VariableMode) :
            this(base.type, base.name, base.arraySize, mode)

    fun flat(): Variable {
        isFlat = true
        return this
    }

    val size = when (type) {
        GLSLType.V1B -> 5
        GLSLType.V1I -> 7
        GLSLType.V1F -> 10
        GLSLType.V2I, GLSLType.V2F -> 20
        GLSLType.V3I, GLSLType.V3F -> 30
        GLSLType.V4I, GLSLType.V4F -> 40
        GLSLType.M2x2 -> 40
        GLSLType.M3x3 -> 90
        GLSLType.M4x3 -> 120
        GLSLType.M4x4 -> 160
        else -> 1000
    } * max(1, arraySize)

    fun declare(code: StringBuilder, prefix: String?, assign: Boolean) {
        if (prefix != null && prefix.startsWith("uniform") && arraySize > 0 && type.glslName.startsWith("sampler")) {
            // define sampler array
            val type = if (!GFX.supportsDepthTextures) type.withoutShadow() else type
            for (index in 0 until arraySize) {
                code.append(prefix).append(' ')
                code.append(type.glslName).append(' ')
                code.append(name).append(index).append(";\n")
            }
        } else {
            // define normal variable
            if (prefix != null) code.append(prefix).append(' ')
            code.append(type.glslName)
            if (arraySize >= 0) {
                code.append('[').append(arraySize).append(']')
            }
            code.append(' ').append(name)
            if (assign) {
                when (type) {
                    GLSLType.V1B -> code.append("=false;\n")
                    GLSLType.V1F -> code.append("=0.0;\n")
                    GLSLType.V2F -> code.append("=vec2(0.0,0.0);\n")
                    GLSLType.V3F -> code.append("=vec3(0.0,0.0,0.0);\n")
                    GLSLType.V4F -> code.append("=vec4(0.0,0.0,0.0,0.0);\n")
                    GLSLType.V1I -> code.append("=0;\n")
                    GLSLType.V2I -> code.append("=ivec2(0,0);\n")
                    GLSLType.V3I -> code.append("=ivec3(0,0,0);\n")
                    GLSLType.V4I -> code.append("=ivec4(0,0,0,0);\n")
                    else -> code.append(";\n")
                }
            } else code.append(";\n")
        }
    }

    fun declare0(code: StringBuilder, prefix: String? = null) {
        if (prefix != null && prefix.startsWith("uniform") && arraySize > 0 && type.glslName.startsWith("sampler")) {
            throw IllegalStateException("Cannot assign to uniform array")
        } else {
            // define normal variable
            if (prefix != null) code.append(prefix).append(' ')
            code.append(type.glslName)
            if (arraySize >= 0) {
                code.append('[').append(arraySize).append(']')
            }
            code.append(' ').append(name)
        }
    }

    var isFlat = false
    var slot = -1

    override fun equals(other: Any?): Boolean {
        return other is Variable && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "${if (isFlat) "flat " else ""}${inOutMode.glslName} ${type.glslName} $name"
    }

    val isAttribute get() = inOutMode == VariableMode.ATTR
    val isInput get() = inOutMode != VariableMode.OUT
    val isOutput get() = inOutMode == VariableMode.OUT || inOutMode == VariableMode.INOUT
    val isModified get() = inOutMode == VariableMode.OUT || inOutMode == VariableMode.INOUT || inOutMode == VariableMode.INMOD
}
