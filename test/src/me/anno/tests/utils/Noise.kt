package me.anno.tests.utils

import me.anno.image.ImageWriter
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import org.apache.logging.log4j.LogManager


// benchmark vs simplex noise
// simplex noise: less computation, more unpredictable branches
fun main() {

    noiseQualityTest()
    // test1D() // 17 ns vs 31 ns (1 ns less for OpenSimplexNoise because of less prediction errors, I'd guess)
    // test2D() // 26 ns vs 32 ns
    // test3D() // 23 ns vs 92 ns
    // test4D() // 48 ns vs 3470 ns (no joke, OpenSimplexNoise in 4d is awful)

    // perlin noise test
    val logger = LogManager.getLogger(PerlinNoise::class)
    val gen = PerlinNoise(1234L, 8, 0.5f, 0f, 1f)
    val samples = 10000
    val buckets = IntArray(10)
    for (i in 0 until samples) {
        buckets[(gen[i.toFloat()] * buckets.size).toInt()]++
    }
    logger.info(buckets.joinToString())
    ImageWriter.writeImageInt(256, 256, false, "perlin.png", 16) { x, y, _ ->
        (gen[x / 100f, y / 100f] * 255).toInt() * 0x10101
    }

}

private fun noiseQualityTest() {
    val noise = FullNoise(1234L)
    ImageWriter.writeImageFloat(512, 512, "n1d.png", 512, false) { x, y, _ ->
        noise[x + y * 512]
    }
    ImageWriter.writeImageFloat(512, 512, "n2d.png", 512, false) { x, y, _ ->
        noise[x, y]
    }
    ImageWriter.writeImageFloat(512, 512, "n3d.png", 512, false) { x, y, _ ->
        noise[x, x - y, y]
    }
    ImageWriter.writeImageFloat(512, 512, "n4d.png", 512, false) { x, y, _ ->
        noise[x + y, x - y, x, y]
    }
}
