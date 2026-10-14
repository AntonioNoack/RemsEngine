package me.anno.ui.canvas

import me.anno.fonts.Font
import me.anno.fonts.FontImpl
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphStyle
import me.anno.fonts.IGlyphLayout
import me.anno.fonts.keys.CharCacheKey
import me.anno.fonts.msdf.MSDFFont
import me.anno.gpu.drawing.DrawTexts.disableSubpixelRendering
import me.anno.gpu.drawing.DrawTexts.isJustALine
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.ui.canvas.CanvasAtlasCache.getBounds
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil
import kotlin.math.max

object CanvasTextDrawHelper : IGlyphLayout() {

    var x = 0
    var y = 0
    var lineHeight = 1

    var color = 0
    var bgColor = 0

    lateinit var font: Font
    lateinit var fontImpl: FontImpl<*>
    lateinit var canvas: Canvas

    override fun add(
        codepoint: Int, x0: Int, x1: Int,
        lineIndex: Int, fontIndex: Int,
        style: Long,
    ) {
        val y0 = lineIndex * lineHeight
        val fontImpl = fontImpl
        if (fontImpl is MSDFFont) {

            val x = x + x0
            val y = y + y0
            if (isJustALine(codepoint)) {
                val rdy = if (codepoint == GlyphStyle.STRIKETHROUGH_CHAR.code) 0.5f else 0.8f
                val dy = (font.size * rdy).roundToIntOr()
                val w = x1 - x0 + 2
                val h = 1
                canvas.drawRect(x, y + dy, w, h, color)
                return
            }

            val index = fontImpl.getIndex(codepoint)
            if (index >= 0) {
                // wait sync?
                val texture = TextureCache[fontImpl.image].value
                if (texture != null && texture.isCreated()) {

                    val index = max(index, 0)
                    val dataScale = font.size
                    val atlasScale = font.size / fontImpl.baseSize

                    // use these for offset
                    val dataX0 = fontImpl.getPlaneLeft(index) * dataScale
                    val dataY1 = fontImpl.getPlaneTop(index) * dataScale

                    val texX0 = fontImpl.getAtlasLeft(index)
                    val texX1 = fontImpl.getAtlasRight(index)
                    val texY0 = fontImpl.getAtlasBottom(index)
                    val texY1 = fontImpl.getAtlasTop(index)

                    val texWidth = ceil((texX1 - texX0) * atlasScale).toIntOr()
                    val texHeight = ceil((texY1 - texY0) * atlasScale).toIntOr()
                    if (texWidth < 1 || texHeight < 1) return

                    val bounds = getBounds(canvas, texture)!!
                    val x0 = (bounds.x0 + texX0).toShort()
                    val y0 = (bounds.y0 + texY0).toShort()
                    val x1 = (bounds.x0 + texX1).toShort()
                    val y1 = (bounds.y0 + texY1).toShort()

                    val dx = dataX0.roundToIntOr()
                    val dy = (font.size * 1.05f - dataY1).roundToIntOr()

                    canvas.pushMSDFText(x0, y0, x1, y1, x + dx, y + dy, texWidth, texHeight)
                }
                return
            }
        }

        // todo should we wait sync??? we should have an option for that...
        val key = CharCacheKey(font, codepoint, disableSubpixelRendering)
        val texture = FontManager.getTexture(key)
            .waitFor("drawTextCharByChar")
        val isJustALine = isJustALine(codepoint)
        if (texture != null && texture.isCreated()) {

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
