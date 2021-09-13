package me.anno.ui.base

import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit

object DefaultRenderingHints {

    val hints: MutableMap<*,*>

    init {
        val hints = (
        Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints") as? Map<*,*>
            ?: mapOf(RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        ).toMutableMap()
        hints[RenderingHints.KEY_FRACTIONALMETRICS] = RenderingHints.VALUE_FRACTIONALMETRICS_ON
        DefaultRenderingHints.hints = hints
    }

    fun Graphics2D.prepareGraphics(font: Font){
        this.font = font
        setRenderingHints(hints)
    }

    fun Graphics2D.prepareGraphics(){
        setRenderingHints(hints)
    }

}