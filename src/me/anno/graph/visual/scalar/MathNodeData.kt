package me.anno.graph.visual.scalar

import me.anno.utils.types.Strings.upperSnakeCaseToTitle

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
    val byId = enumValues.associateBy { getId(it) }
    val typeToIndex = enumValues.withIndex().associate { it.value to it.index }
}