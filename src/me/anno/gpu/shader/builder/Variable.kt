package me.anno.gpu.shader.builder

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
