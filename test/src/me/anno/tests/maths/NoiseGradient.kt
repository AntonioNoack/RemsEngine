package me.anno.tests.maths

import me.anno.config.DefaultConfig.style
import me.anno.graph.ui.GraphPanel.Companion.greenish
import me.anno.maths.noise.PerlinNoise
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.utils.Function1d
import me.anno.ui.utils.FunctionPanel
import me.anno.utils.Color.black
import org.joml.Vector4f

fun main() {
    val noise = PerlinNoise(1234L, 5, 0.5f, -1f, +1f, Vector4f(0.5f))
    testUI3("Noise + Gradient") {
        FunctionPanel(
            listOf(
                Function1d { noise.getSmoothGradient(it.toFloat()).toDouble() } to (greenish or black),
                Function1d { noise.getSmooth(it.toFloat()).toDouble() } to -1,
            ), style
        )
    }
}