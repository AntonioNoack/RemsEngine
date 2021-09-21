package me.anno.gpu.monitor

import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3i
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

object SubpixelLayout {

    private val LOGGER = LogManager.getLogger(SubpixelLayout::class)

    private fun add(sum: Vector3i, x: Int, y: Int, v: Int) {
        sum.add(x * v, y * v, v)
    }

    private fun mean(v: Vector3i): Vector2f {
        return Vector2f(v.x.toFloat() / v.z, v.y.toFloat() / v.z)
    }

    val r: Vector2f
    val g: Vector2f
    val b: Vector2f

    init {

        // a dot is a simple circle:
        // we will have a simple transition example

        val width = 10
        val height = 10

        val image = BufferedImage(width, height, 1)

        val gfx = image.graphics as Graphics2D
        gfx.font = gfx.font.deriveFont(width)
        gfx.background = Color.BLACK
        gfx.color = Color.WHITE
        gfx.prepareGraphics()
        gfx.drawString(".", 3, height * 4 / 5)
        gfx.dispose()

        /*FileReference.getReference(OS.desktop, "dot.png").outputStream().use {
            ImageIO.write(image, "png", it)
        }*/

        val rs = Vector3i()
        val gs = Vector3i()
        val bs = Vector3i()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = image.getRGB(x, y)
                add(rs, x, y, rgb.r())
                add(gs, x, y, rgb.g())
                add(bs, x, y, rgb.b())
            }
        }

        if (rs.z == 0 || gs.z == 0 || bs.z == 0) {

            LOGGER.warn("Could not find/draw dot to find subpixel arrangement")

            r = Vector2f()
            g = Vector2f()
            b = Vector2f()

        } else {

            r = mean(rs)
            g = mean(gs)
            b = mean(bs)

            val mean = Vector2f(r).add(g).add(b).div(3f)
            r.sub(mean)
            g.sub(mean)
            b.sub(mean)

            LOGGER.info("Subpixel arrangement: ${r.print()}, ${g.print()}, ${b.print()}")

        }

    }

}