package me.anno.tests.ui

import me.anno.fonts.FontManager
import me.anno.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import java.awt.Color
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {

    val te = "Te"
    val t = "T"
    val e = "e"

    val w = 512

    val fs = w / 5f
    val font = FontManager.getFont("Verdana", fs, bold = false, italic = false).awtFont
    val img = BufferedImage(w, w, 1)
    val gfx = img.graphics as Graphics2D
    gfx.prepareGraphics(font, false) // yes, values are changing: single letters are assigned fraction widths now too

    val ctx = FontRenderContext(null, true, true)//gfx.fontRenderContext
    val sl = TextLayout(".", font, ctx)

    fun width(str: String) = TextLayout(str, font, ctx).advance

    /*(width(te))
    (width(t))
    (width(e))*/

    gfx.translate(3, 3)

    val fh = sl.ascent + sl.descent
    fun drawString(str: String, x: Float) {
        val wi = width(str)
        gfx.color = Color.DARK_GRAY
        gfx.drawRect(x.toInt(), 0, wi.toInt(), fh.toInt())
        gfx.color = Color.WHITE
        gfx.drawString(str, x, fs)
    }

    // correct calculation, but wrong spacing
    drawString(te, 0f)
    gfx.translate(0, fs.toInt())

    // wrong spacing, drawn
    drawString(t, 0f)
    drawString(e, width(t))
    gfx.translate(0, fs.toInt())

    val correctOffset = width(te) - width(e)
    drawString(t, 0f)
    drawString(e, correctOffset)
    gfx.translate(0, fs.toInt())

    gfx.dispose()
    OS.desktop.getChild("font2.png").outputStream().use {
        ImageIO.write(img, "png", it)
    }

}