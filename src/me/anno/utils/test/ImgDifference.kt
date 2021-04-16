package me.anno.utils.test

import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Maths
import me.anno.utils.OS
import me.anno.utils.image.ImageWriter
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

fun main() {

    val folder = File(OS.pictures.file, "Screenshots")
    val i0 = ImageIO.read(File(folder, "i0.png"))
    val i1 = ImageIO.read(File(folder, "i1.png"))

    val w = Maths.min(i0.width, i1.width)
    val h = Maths.min(i0.height, i1.height)

    ImageWriter.writeRGBImageInt(w, h, "iDiff.png", 512) { x, y, _ ->
        val c0 = i0.getRGB(x, y)
        val c1 = i1.getRGB(x, y)
        rgba(abs(c0.r() - c1.r()), abs(c0.g() - c1.g()), abs(c0.b() - c1.b()), 255)
    }

}