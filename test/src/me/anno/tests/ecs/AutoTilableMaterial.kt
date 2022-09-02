package me.anno.tests.ecs

import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.ecs.components.shaders.AutoTileableShader
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.GFXBase
import me.anno.maths.LinearRegression
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.pictures
import org.joml.Vector2d
import kotlin.math.exp
import kotlin.math.pow

fun main() {
    // test for auto-tileable material
    GFXBase.forceLoadRenderDoc()
    testUI {
        val material = AutoTileableMaterial()
        material.diffuseMap = pictures.getChild("textures/grass.jpg")
        testScene(material)
    }

    fun test() {
        // todo erf() is just a polynomial, and erfInv is just trying to traverse that polynomial;
        //  surely, we can express truncCdfInv as some simple polynomial
        //  and it's used for graphics, so slight errors shouldn't be too bad
        /*fun truncCdfInv2(x: Float): Float {// fitted curve by eye
            val v = x - 0.5f
            return 0.5f + 0.42f * v + 32f * v.pow(7) - sin(v * 3f * PIf) / 60f
        }*/
        val points = Array(1001) {
            val raw = it / 1000f
            // more weights at the edges
            val densityMapped = 1f / (1f + exp(-(raw - .5f) * 20f))
            Vector2d(
                (densityMapped - 0.5) * 2.0,
                (AutoTileableShader.TileMath.truncCdfInv(densityMapped) - 0.5)
            )
        }.toList()
        val funcs: List<(Double) -> Double> =
            listOf({ it }, { it.pow(3) }, { it.pow(11) })
        val polynomial = LinearRegression.solve(points, funcs)!!
        println("weights: ${polynomial.joinToString()}")
        // todo draw these points with error bars...
        for (p in points) {
            println("$p -> ${LinearRegression.evaluatePolynomial(p.x, polynomial, funcs)}")
        }
    }

}