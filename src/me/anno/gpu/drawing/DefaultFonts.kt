package me.anno.gpu.drawing

import me.anno.config.DefaultConfig
import me.anno.fonts.Font
import me.anno.fonts.FontManager

object DefaultFonts {

    private fun findMonospaceFont(): String {
        val fonts = FontManager.fontSet
        return when {
            "Consolas" in fonts -> "Consolas" // best case
            "Courier New" in fonts -> "Courier New" // second best case
            else -> fonts.firstOrNull { it.contains("mono", true) }
                ?: fonts.firstOrNull()
                ?: "Courier New"
        }
    }

    val monospaceFont by lazy {
        val size = DefaultConfig.style.getSize("fontSize", 12)
        val bold = false
        val italic = false
        val fontName = findMonospaceFont()
        Font(fontName, size, bold, italic)
    }

}