package me.anno.gpu.shader.builder

class ShaderStage(
    val callName: String,
    val variables: List<Variable>,
    val body: String
) {

    val attributes get() = variables.filter { it.inOutMode == VariableMode.ATTR }

    val functions = ArrayList<Function>()

    val defines = ArrayList<String>()

    fun define(value: String): ShaderStage {
        defines += value
        return this
    }

}