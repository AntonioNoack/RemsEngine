package me.anno.graph.render

import me.anno.Time
import me.anno.graph.types.flow.CalculationNode

class GameTime : CalculationNode("Game Time (%1h,FP32)", emptyList(), listOf("Float", "Game Time")) {
    // repeats once every 1h to ensure relatively consistent accuracy
    // if you need more accuracy, create your own nodes, but remember that OpenGL uses single precision!
    override fun calculate() = (Time.gameTime % 3600f).toFloat()
}