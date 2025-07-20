package me.anno.graph.visual.scalar

import me.anno.utils.types.Strings.upperSnakeCaseToTitle
import speiger.primitivecollections.IntToObjectHashMap
import speiger.primitivecollections.ObjectToIntHashMap

class MathNodeData<V : Enum<V>>(
    val enumValues: List<V>,
    inputTypes: List<String>,
    outputType: String,
    getId: (V) -> Int,
    val getGLSL: (V) -> String,
    val names: List<String> = enumValues.map {
        "${inputTypes.first()} ${it.name.upperSnakeCaseToTitle()}"
    }
) {
    val inputs = inputTypes.flatMapIndexed { i, type ->
        listOf(type, ('A' + i).toString())
    }
    val defaultType = enumValues[0]
    val outputs = listOf(outputType, "Result")
    val byId = createIdToEnumMap(enumValues, getId)
    val typeToIndex = createTypeToIndexMap(enumValues)

    companion object {

        @JvmStatic
        private fun <V> createIdToEnumMap(enumValues: List<V>, getId: (V) -> Int): IntToObjectHashMap<V> {
            val map = IntToObjectHashMap<V>(enumValues.size)
            for (i in enumValues.indices) {
                val value = enumValues[i]
                map[getId(value)] = value
            }
            return map
        }

        @JvmStatic
        private fun <V> createTypeToIndexMap(enumValues: List<V>): ObjectToIntHashMap<V> {
            val map = ObjectToIntHashMap<V>(-1, enumValues.size)
            for (i in enumValues.indices) {
                map[enumValues[i]] = i
            }
            return map
        }
    }
}