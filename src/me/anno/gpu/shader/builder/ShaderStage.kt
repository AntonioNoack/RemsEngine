package me.anno.gpu.shader.builder

class ShaderStage(
    val callName: String,
    variables: List<Variable>,
    val body: String
) {

    constructor(callName: String, variables: List<Variable>, varyings: List<Variable>, vertex: Boolean, body: String) :
            this(
                callName,
                variables + varyings.map { Variable(it, if (vertex) VariableMode.OUT else VariableMode.IN) },
                extractMain(body)
            ) {
        extractFunctions(body)
    }

    var variables: List<Variable> = variables
        private set

    val attributes get() = variables.filter { it.inOutMode == VariableMode.ATTR }
    private val variablesByName by lazy { variables.groupBy { it.name } }

    fun getVariablesByName(name: String): List<Variable> {
        return variablesByName[name] ?: emptyList()
    }

    fun addVariables(variable: List<Variable>) {
        variables += variable
    }

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

    operator fun plus(other: ShaderStage): List<ShaderStage> {
        return listOf(this, other)
    }

    operator fun plus(other: List<ShaderStage>): List<ShaderStage> {
        return listOf(this) + other
    }

    companion object {
        @JvmStatic
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