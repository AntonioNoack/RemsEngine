package me.anno.ui.base

import java.awt.RenderingHints
import java.awt.Toolkit

object DefaultRenderingHints {

    val hints = (
            Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints") as? Map<*,*>
                ?: mapOf(
                    RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
                )
            ).toMutableMap()

    init {
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    }

}