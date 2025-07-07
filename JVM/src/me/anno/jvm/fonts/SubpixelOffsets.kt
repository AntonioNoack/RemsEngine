package me.anno.jvm.fonts

import me.anno.fonts.FontStats.subpixelOffsetB
import me.anno.fonts.FontStats.subpixelOffsetR
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import org.joml.Vector2f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.max

object SubpixelOffsets {

    fun calculateSubpixelOffsets() { // ~22 ms
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        val gfx = img.graphics as Graphics2D
        val font = gfx.font.deriveFont(16f)
        gfx.prepareGraphics(font, false)
        gfx.background = Color.BLACK
        gfx.color = Color.WHITE
        gfx.clearRect(0, 0, img.width, img.height)
        gfx.drawString("O", 0, 16 - 2)
        gfx.dispose() // push
        val pixels = IntArray(img.width * img.height)
        img.getRGB(0, 0, img.width, img.height, pixels, 0, img.width)
        val center = Vector2f()
        getMediumPos(center, pixels, img.width, img.height, 8)
        getMediumPos(subpixelOffsetR, pixels, img.width, img.height, 16)
        getMediumPos(subpixelOffsetB, pixels, img.width, img.height, 0)
        center.sub(subpixelOffsetR, subpixelOffsetR)
        center.sub(subpixelOffsetB, subpixelOffsetB)
    }

    private fun getMediumPos(sum: Vector2f, pixels: IntArray, w: Int, h: Int, shift: Int): Vector2f {
        var weightSum = 0f
        var i = 0
        sum.set(0f)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val weight = pixels[i++].shr(shift).and(0xff).toFloat()
                sum.add(weight * x, weight * y)
                weightSum += weight
            }
        }
        sum.div(max(weightSum, 1f))
        return sum
    }
}