package me.anno.objectsV2.components.shaders

import me.anno.objectsV2.Component

class ShaderEnvironment {

    var parent: ShaderEnvironment? = null

    val variables = HashMap<Any, String>()

    val usedVariables = HashMap<String, Int>()

    fun getVariable(key: Component, name: String, type: VariableType): String {

        val i0 = name.lastIndexOf('_')
        if (i0 >= 0) {
            if (name.substring(i0 + 1).toIntOrNull() != null) {
                throw IllegalArgumentException("_<number> is not allowed, as it's used internally")
            }
        }

        val joinedKey = Triple(key.getClassName(), name, type)
        val cached = variables[joinedKey]
        if (cached != null) return cached
        val count = usedVariables[name] ?: 0
        val newName = "${name}_$count"

        variables[joinedKey] = newName
        usedVariables[name] = count + 1

        return newName

    }

    operator fun get(key: Component, name: String, type: VariableType) = getVariable(key, name, type)


}