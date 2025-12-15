package me.anno.tests.maths.noise

import me.anno.image.raw.IntImage
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.WorleyCell
import me.anno.maths.noise.WorleyNoise
import me.anno.utils.Color.rgb
import me.anno.utils.OS.desktop

fun main() {
    val noise = WorleyNoise(1234)
    val cell = WorleyCell()
    val size = 256
    val scale = 1f / 20f
    val image = IntImage(size, size, false)
    val rndX = FullNoise(13245)
    val rndY = FullNoise(86412)
    image.forEachPixel { x, y ->
        noise.getDistanceSq(x * scale, y * scale, cell)
        val r = rndX[cell.xi, cell.yi]
        val g = rndY[cell.xi, cell.yi]
        image.setRGB(x, y, rgb(r, g, 1f))
    }
    image.write(desktop.getChild("WorleyBreak.png"))
}