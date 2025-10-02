package me.anno.tests.gfx

import me.anno.ecs.components.light.sky.Skybox.Companion.fbmSmoothstepAverage
import me.anno.image.ImageWriter
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import org.joml.Vector2f

/**
 * On the horizon, we want to avoid sampling the clouds, because we get trouble with the sampling theorem.
 * Instead, we use a well-chosen average.
 *
 * Computing this average isn't as simple as it sounds: the density is given by smoothstep(1-x, 1, fbmNoise()).
 * */
fun main() {
    println(fbmSmoothstepAverage(0.0f))
    println(fbmSmoothstepAverage(0.5f))
    println(fbmSmoothstepAverage(1.0f))
    println(fbmSmoothstepAverage(1.5f))
    println(fbmSmoothstepAverage(2.0f))

    val n = 512
    ImageWriter.writeImageCurve(
        1024, 1024, true, false,
        black, white, 2, List(n) {
            val f = it / (n - 1f)
            val x = f * 2f
            val y = -fbmSmoothstepAverage(x)
            Vector2f(x, y)
        }, "fbmSmoothstepAverage.png"
    )
}