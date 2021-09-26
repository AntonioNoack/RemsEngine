package me.anno.graph.types.flow

abstract class GetterNode : CalculationNode {

    constructor(name: String) : super(name)

    constructor(name: String, outputType: String) : super(name, emptyList(), listOf(outputType))

}