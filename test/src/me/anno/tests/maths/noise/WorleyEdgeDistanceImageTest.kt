package me.anno.tests.maths.noise

import me.anno.image.raw.FloatImage
import me.anno.maths.noise.WorleyCell
import me.anno.maths.noise.WorleyNoise
import me.anno.utils.OS.desktop

fun main() {
    val noise = WorleyNoise(1234)
    val cell = WorleyCell()
    val size = 1000
    val scale = 1f / 10f
    val image = FloatImage(size, size, 1)
    image.forEachPixel { x, y ->
        val dist = noise.getDistanceToEdge(x * scale, y * scale, cell)
        image.setValue(x, y, 0, dist)
    }
    image.write(desktop.getChild("WorleyEdgeDistance.png"))
}