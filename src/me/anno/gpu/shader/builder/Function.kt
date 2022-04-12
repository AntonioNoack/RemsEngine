package me.anno.gpu.shader.builder

import me.anno.Build

data class Function(
    val name: String,
    val header: String,
    val body: String
) {

    constructor(body: String) : this("", "", body)

    constructor(name: String, variables: List<Variable>, body: String) : this(
        name,
        join(name, variables),
        join(name, variables, body)
    )

    companion object {

        private val separator = if (Build.isDebug) ", " else ","

        fun join(name: String, variables: List<Variable>): String {
            return variables.joinToString(separator, "void $name(", ")") { it.type.glslName }
        }

        fun join(name: String, variables: List<Variable>, body: String): String {
            return variables.joinToString(separator, "void $name(", "){\n$body}\n") {
                if (it.arraySize >= 0) {
                    "${it.inOutMode.glslName} ${it.type.glslName} ${it.name}[${it.arraySize}]"
                } else {
                    "${it.inOutMode.glslName} ${it.type.glslName} ${it.name}"
                }
            }
        }

    }

}
