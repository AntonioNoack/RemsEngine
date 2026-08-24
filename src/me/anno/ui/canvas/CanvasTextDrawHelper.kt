package me.anno.ui.canvas

import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphStyle
import me.anno.fonts.IGlyphLayout
import me.anno.fonts.keys.CharCacheKey
import me.anno.gpu.drawing.DrawTexts.disableSubpixelRendering
import me.anno.gpu.drawing.DrawTexts.isJustALine
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.ui.canvas.CanvasAtlasCache.getBounds
import me.anno.utils.types.Floats.roundToIntOr

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
        val isJustALine = isJustALine(codepoint)
        if (texture != null && texture.wasCreated) {

            val x = x + x0
            var y = y + y0
            if (isJustALine) {
                val dy = if (codepoint == GlyphStyle.STRIKETHROUGH_CHAR.code) 0.5f else 0.8f
                y += (texture.height * dy).roundToIntOr()
            }

            val w = if (isJustALine) x1 - x0 + 2 else texture.width
            val h = texture.height

            val bounds = getBounds(canvas, texture)
            if (bounds != null) {
                canvas.pushText(bounds, x, y, w, h)
            } else {
                canvas.custom {
                    // big text?? todo can we draw properly?
                    texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
                    canvas.drawTexture(x, y, w, h, texture, false, color)
                }
            }
        }
    }

    override fun move(dx: Int, deltaLineWidth: Int) {}
    override fun finishLine(i0: Int, i1: Int, lineWidth: Int) {}
}
