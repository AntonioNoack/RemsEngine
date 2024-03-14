package me.anno.tests.gfx

import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.image.ImageCache
import me.anno.image.ImageWriter
import me.anno.maths.Maths
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.OS.screenshots
import kotlin.math.abs

fun main() {

    OfficialExtensions.register()
    ExtensionLoader.load()

    val i0 = ImageCache[screenshots.getChild("i0.png"), false]!!
    val i1 = ImageCache[screenshots.getChild("i1.png"), false]!!

    val w = Maths.min(i0.width, i1.width)
    val h = Maths.min(i0.height, i1.height)

    ImageWriter.writeRGBImageInt(w, h, "iDiff.png", 512) { x, y, _ ->
        val c0 = i0.getRGB(x, y)
        val c1 = i1.getRGB(x, y)
        rgba(abs(c0.r() - c1.r()), abs(c0.g() - c1.g()), abs(c0.b() - c1.b()), 255)
    }
}