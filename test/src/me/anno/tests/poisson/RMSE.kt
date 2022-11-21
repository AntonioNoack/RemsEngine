package me.anno.tests.poisson

import me.anno.image.ImageCPUCache
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.pictures
import kotlin.math.sqrt

fun main() {

    val files = pictures.getChild("Screenshots")
        .listChildren()!!
        .map {
            Pair(it, it.name.filter { it in '0'..'9' }.toIntOrNull())
        }
        .filter { it.second in 311..319 }
        .sortedBy { it.second }
        .map { ImageCPUCache[it.first, false]!! }

    val baseline = files.first()
    val samples = files.subList(1, files.size)

    val x = 243
    val y = 145
    val w = 1523 - x
    val h = 865 - y

    println("$w,$h")

    for (sample in samples) {
        var error = 0.0
        for (yi in y until y + h) {
            var err = 0.0
            for (xi in x until x + w) {
                val c0 = baseline.getRGB(xi, yi)
                val c1 = sample.getRGB(xi, yi)
                val dr = c0.r01() - c1.r01()
                val dg = c0.g01() - c1.g01()
                val db = c0.b01() - c1.b01()
                err += dr * dr + dg * dg + db * db
            }
            error += err // reduce accumulated errors
        }
        error /= (w * h)
        error = sqrt(error)
        println(error)
    }

    // todo create joined image
    

}