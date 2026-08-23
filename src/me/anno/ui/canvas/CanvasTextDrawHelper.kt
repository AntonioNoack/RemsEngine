package me.anno.ui.canvas

import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.IGlyphLayout
import me.anno.fonts.keys.CharCacheKey
import me.anno.gpu.drawing.DrawTexts.disableSubpixelRendering
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering

object CanvasTextDrawHelper : IGlyphLayout() {

    var x = 0
    var y = 0
    var lineHeight = 1

    var color = 0
    var bgColor = 0

    lateinit var font: Font
    lateinit var canvas: Canvas

    override fun add(
        codepoint: Int, x0: Int, x1: Int,
        lineIndex: Int, fontIndex: Int,
        style: Long,
    ) {
        val y0 = lineIndex * lineHeight
        val key = CharCacheKey(font, codepoint, disableSubpixelRendering)
        val texture = FontManager.getTexture(key)
            .waitFor("drawTextCharByChar")
        if (texture != null && texture.wasCreated) {
            // todo somehow encode bgColor
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            canvas.drawTexture(x + x0, y + y0, x1 - x0, texture.height, texture, color)
        }
    }

    override fun move(dx: Int, deltaLineWidth: Int) {}
    override fun finishLine(i0: Int, i1: Int, lineWidth: Int) {}
}
