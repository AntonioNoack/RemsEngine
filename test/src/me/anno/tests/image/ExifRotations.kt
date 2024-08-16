package me.anno.tests.image

import me.anno.image.jpg.ExifOrientation
import me.anno.utils.OS

fun main() {
    val folder = OS.documents.getChild("IdeaProjects/RemsEngine/progress/exif")
    for (i in 1..8) {
        val src = folder.getChild("orientation_$i.jpg")

        // find manually
        // Exif\u00[padding \u00][II|MM for byte-order, II = Little Endian, MM = Big Endian][skip 6][*uint16 num fields],
        // fields: each 12 bytes in size, [uint16 tag, must be 0x0112][uint16 type][uint32 count][uint16 actual value :)]
        // int16u, group IFD0,
        // 1 = normal, 2 = mirror x,
        // 3 = rotate 180°, 4 = mirror y,
        // 5 = mirror x + rotate 270° cw,
        // 6 = rotate 90° cw, 7 = mirror x and rotate 90° cw,
        // 8 = rotate 270° cw

        // before we had implemented and checked our version :)
        // println(ImageData.getRotation(src))
        ExifOrientation.findRotation(src) { rot, _ ->
            println("${src.nameWithoutExtension}: $rot")
        }
    }
}
