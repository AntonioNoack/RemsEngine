package me.anno.tests.image

import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.utils.Color.g
import me.anno.utils.Color.hex24
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.OS.desktop

fun main() {
    val image = ImageCache[OS.pictures.getChild("RemsStudio/8c841f59b8dedb0b63abcac91cb82392-1000.jpg"), false]!!
    val intImage = image.createIntImage()
    val sortedY = intImage.data.sortedBy { it.g() }
    val sortedX = (0 until image.height).map { y ->
        val i0 = y * image.width
        sortedY.subList(i0, i0 + image.width).sortedBy { it.r() }
    }.flatten().toIntArray()
    val sorted = IntImage(image.width, image.height, sortedX, false)
    sorted.write(desktop.getChild("sorted.png"))
    val corners = sampleCorners(sorted, 6, 6)
    corners.write(desktop.getChild("corners.png"))
    printSmallImage(corners)
}

fun sampleCorners(image: Image, w: Int, h: Int): IntImage {
    val dst = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            dst[x + y * w] = image.getRGB(
                x * (image.width - 1) / (w - 1),
                y * (image.height - 1) / (h - 1)
            )
        }
    }
    return IntImage(w, h, dst, false)
}

fun printSmallImage(image: Image) {
    println("IntImage(${image.width}, ${image.height}, intArrayOf(")
    for (y in 0 until image.height) {
        println((0 until image.width).joinToString { x ->
            "0x${hex24(image.getRGB(x, y))}"
        } + (if (y + 1 < image.height) ", " else ""))
    }
    println("), false)")
}