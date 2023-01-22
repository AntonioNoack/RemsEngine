package me.anno.graph.render

import me.anno.Engine
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode

class GameTime : CalculationNode("Game Time", emptyList(), listOf("Float", "Game Time")) {
    override fun calculate(graph: FlowGraph) = Engine.gameTimeF
}