package me.anno.graph.types.flow

abstract class GetterNode : CalculationNode {

    constructor() : super()

    constructor(outputType: String) : super(emptyList(), listOf(outputType))

}