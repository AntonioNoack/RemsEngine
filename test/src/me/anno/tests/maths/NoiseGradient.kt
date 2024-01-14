package me.anno.tests.maths

import me.anno.config.DefaultConfig.style
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.ui.UIColors.fireBrick
import me.anno.maths.noise.PerlinNoise
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.Function1d
import me.anno.ui.utils.FunctionPanelNd
import me.anno.utils.Color.black
import org.joml.Vector4f

fun main() {
    val noise = PerlinNoise(1234L, 5, 0.5f, -1f, +1f, Vector4f(0.5f))
    testUI3("Noise + Gradient") {
        FunctionPanelNd(
            listOf(
                // check for gradient, will be invisible unless zoomed in far
                Function1d {
                    val epsilon = 1e-3
                    (noise.getSmooth((it + epsilon).toFloat()) - noise.getSmooth((it - epsilon).toFloat())) / (2 * epsilon)
                } to (fireBrick or black),
                Function1d { noise.getSmoothGradient(it.toFloat()).toDouble() } to (mediumAquamarine or black),
                Function1d { noise.getSmooth(it.toFloat()).toDouble() } to -1,
            ), style
        )
    }
}