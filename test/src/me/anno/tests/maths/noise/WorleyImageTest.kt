package me.anno.tests.maths.noise

import me.anno.image.raw.IntImage
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.WorleyCell
import me.anno.maths.noise.WorleyNoise
import me.anno.utils.OS.desktop

fun main() {
    val noise = WorleyNoise(1234)
    val cell = WorleyCell()
    val size = 1000
    val scale = 1f / 10f
    val image = IntImage(size, size, false)
    val rnd = FullNoise(13245)
    image.forEachPixel { x, y ->
        noise.getDistanceSq(x * scale, y * scale, cell)
        val rgb = rnd[cell.xi, cell.yi].toRawBits() * 82438679
        image.setRGB(x, y, rgb)
    }
    image.write(desktop.getChild("Worley.png"))
}