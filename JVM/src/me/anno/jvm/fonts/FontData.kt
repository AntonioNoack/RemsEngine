package me.anno.jvm.fonts

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

class FontData(val awtFont: Font) {

    fun deriveFont(style: Int, size: Float): FontData {
        return FontData(awtFont.deriveFont(style, size))
    }

    val renderContext = FontRenderContext(null, true, true)

    val baselineY: Float
    val lineHeight: Float

    init {
        val exampleLayout = TextLayout("o", awtFont, renderContext)
        baselineY = exampleLayout.ascent
        lineHeight = exampleLayout.ascent + exampleLayout.descent
    }
}