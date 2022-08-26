package me.anno.gpu.shader.builder

class ShaderStage(
    val callName: String,
    val variables: List<Variable>,
    val body: String
) {

    constructor(variables: List<Variable>, body: String) :
            this("main1", variables, body)

    constructor(variables: List<Variable>, varyings: List<Variable>, vertex: Boolean, body: String) :
            this(
                "main2",
                variables + varyings.map { Variable(it, if (vertex) VariableMode.OUT else VariableMode.IN) },
                extractMain(body)
            ) {
        extractFunctions(body)
    }

    val attributes get() = variables.filter { it.inOutMode == VariableMode.ATTR }

    val functions = ArrayList<Function>()

    val defines = ArrayList<String>()

    fun add(func: Function): ShaderStage {
        functions.add(func)
        return this
    }

    fun add(func: String): ShaderStage {
        functions.add(Function(func))
        return this
    }

    fun define(value: String): ShaderStage {
        defines += value
        return this
    }

    fun extractFunctions(str: String) {
        if (str.length < 13) return
        val idx = str.indexOf("void main()")
        val idx2 = str.indexOf('{', idx + 9) + 1
        val idx3 = str.lastIndexOf('}')
        if (idx2 in 0 until idx3) {
            functions.add(Function(str.substring(0, idx)))
        }
    }

    companion object {
        fun extractMain(str: String): String {
            if (str.length < 13) return str
            val idx = str.indexOf("void main()")
            val idx2 = str.indexOf('{', idx + 9) + 1
            val idx3 = str.lastIndexOf('}')
            return if (idx2 in 0 until idx3) str.substring(idx2, idx3)
            else str
        }
    }

}