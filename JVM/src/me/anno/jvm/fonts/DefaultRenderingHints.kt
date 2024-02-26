package me.anno.jvm.fonts

import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit

object DefaultRenderingHints {

    private val LOGGER = LogManager.getLogger(DefaultRenderingHints::class)

    val hints: MutableMap<*, *>

    // display-independent hints, without subpixel rendering: scalable without color artefacts
    val portableHints = mutableMapOf(
        RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_ON,
        RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON
    )

    init {
        val desktopHints = if(OS.isWindows) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")
        else null // crashes on Linux Mint with a segfault, so I'd guess it's only fine on Windows
        LOGGER.info("Hints for font rendering: $desktopHints")
        val hints = (desktopHints as? Map<*, *>
            ?: mapOf(RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
                ).toMutableMap()
        hints.putAll(portableHints)
        DefaultRenderingHints.hints = hints
    }

    fun Graphics2D.prepareGraphics(font: Font, portableImages: Boolean) {
        this.font = font
        prepareGraphics(portableImages)
    }

    fun Graphics2D.prepareGraphics(portableImages: Boolean) {
        setRenderingHints(if (portableImages) portableHints else hints)
    }

}