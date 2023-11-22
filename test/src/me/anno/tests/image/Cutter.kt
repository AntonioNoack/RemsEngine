package me.anno.tests.image

import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Color.white
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import kotlin.math.min

// for my thesis: merging images
fun main() {

    val images = pictures.getChild("Screenshots")
        .listChildren()!!
        .map { file ->
            val number = file.name.filter { it in '0'..'9' }.toIntOrNull()
            Pair(number, file)
        }
        .filter { it.first in 268..273 }
        .sortedBy { it.first }
        .map { ImageCache[it.second, false]!! }

    val bx = 563
    val by = 285
    val bw = 600
    val bh = 600

    val base = images.first()
    val space = bw / 6

    val rows = 2
    val cols = ceilDiv(images.size, rows)

    val w = (bw + space) * cols - space
    val h = (bh + space) * rows - space

    val newImage = IntImage(w, h, base.hasAlphaChannel)
    newImage.data.fill(white)
    for (i in images.indices) {
        val img = images[i]
        val x0 = (bw + space) * (i % cols)
        val y0 = (bh + space) * (i / cols)
        for (y in 0 until min(bh, img.height - by)) {
            for (x in 0 until min(bw, img.width - bx)) {
                newImage.setRGB(x + x0, y + y0, img.getRGB(bx + x, by + y))
            }
        }
    }

    newImage.write(desktop.getChild("combined.png"))

}