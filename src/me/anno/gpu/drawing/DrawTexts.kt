package me.anno.gpu.drawing

import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.FrameTimes
import me.anno.utils.maths.Maths
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.roundToInt

object DrawTexts {

    private val LOGGER = LogManager.getLogger(DrawTexts::class)

    val simpleChars = Array('z'.code + 1) { it.toChar().toString() }
    var monospaceFont = lazy { Font("Consolas", DefaultConfig.style.getSize("fontSize", 12), false, false) }
    val monospaceKeys =
        lazy { Array(simpleChars.size) { FontManager.getTextCacheKey(monospaceFont.value, simpleChars[it], -1, -1) } }

    fun drawSimpleTextCharByChar(
        x0: Int, y0: Int,
        padding: Int,
        text: CharArray,
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignmentX: AxisAlignment = AxisAlignment.MIN
    ) {
        val font = monospaceFont.value
        val keys = monospaceKeys.value
        val charWidth = font.sampleWidth
        val size = text.size
        val width = charWidth * size
        val offset = -when (alignmentX) {
            AxisAlignment.MIN, AxisAlignment.FILL -> 0
            AxisAlignment.CENTER -> width / 2
            AxisAlignment.MAX -> width
        }
        DrawRectangles.drawRect(
            x0 + offset, y0,
            charWidth * text.size + 2 * padding, font.sizeInt + 2 * padding,
            backgroundColor
        )
        for (i in text.indices) {
            val char = text[i]
            val charInt = char.code
            if (charInt < simpleChars.size) {
                val key = keys[charInt] ?: continue
                drawText(
                    x0 + offset + padding + i * charWidth, y0 + padding,
                    font, key, textColor, backgroundColor.and(0xffffff)
                )
            }
        }
    }


    fun drawSimpleTextCharByChar(
        x0: Int, y0: Int,
        padding: Int,
        text: String,
        alignment: AxisAlignment = AxisAlignment.MIN
    ): Unit = drawSimpleTextCharByChar(
        x0, y0, padding, text, FrameTimes.textColor,
        FrameTimes.backgroundColor, alignment
    )

    fun drawSimpleTextCharByChar(
        x0: Int, y0: Int,
        padding: Int,
        text: String,
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignment: AxisAlignment = AxisAlignment.MIN
    ) {
        GFX.check()
        val font = monospaceFont.value
        val keys = monospaceKeys.value
        val charWidth = font.sampleWidth
        val size = text.length
        val width = charWidth * size
        val offset = -when (alignment) {
            AxisAlignment.MIN, AxisAlignment.FILL -> 0
            AxisAlignment.CENTER -> width / 2
            AxisAlignment.MAX -> width
        }
        DrawRectangles.drawRect(
            x0 + offset, y0,
            charWidth * text.length + 2 * padding, font.sizeInt + 2 * padding,
            backgroundColor
        )
        for (i in text.indices) {
            val char = text[i]
            val charInt = char.code
            if (charInt < simpleChars.size) {
                val key = keys[charInt] ?: continue
                drawText(
                    x0 + offset + padding + i * charWidth, y0 + padding,
                    font, key, textColor, backgroundColor.and(0xffffff)
                )
            }
        }
    }

    fun drawTextCharByChar(
        x: Int, y: Int,
        font: Font,
        text: String,
        color: Int,
        backgroundColor: Int,
        widthLimit: Int,
        heightLimit: Int,
        alignment: AxisAlignment,
        equalSpaced: Boolean
    ): Int {

        // todo correct char distances for everything

        if ('\n' in text) {
            var sizeX = 0
            val split = text.split('\n')
            val lineOffset = font.sizeInt * 3 / 2
            for (index in split.indices) {
                val s = split[index]
                val size = drawTextCharByChar(
                    x, y + index * lineOffset, font, s,
                    color, backgroundColor,
                    widthLimit, heightLimit, alignment, equalSpaced
                )
                sizeX = Maths.max(GFXx2D.getSizeX(size), sizeX)
            }
            return GFXx2D.getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        val charWidth = if (equalSpaced) getTextSizeX(font, "x", widthLimit, heightLimit) else 0

        val offset = when (alignment) {
            AxisAlignment.MIN, AxisAlignment.FILL -> 0
            AxisAlignment.CENTER -> -charWidth * text.length / 2
            AxisAlignment.MAX -> -charWidth * text.length
        }

        if (offset != 0) {
            drawTextCharByChar(
                x + offset, y,
                font, text, color, backgroundColor,
                widthLimit, heightLimit, AxisAlignment.MIN, equalSpaced
            )
        }

        // todo width limit...

        val shader = ShaderLib.subpixelCorrectTextShader.value
        shader.use()
        shader.v4("textColor", color)
        shader.v4("backgroundColor", backgroundColor)

        GFX.loadTexturesSync.push(true)

        var fx = x
        var h = font.sizeInt
        for (char in text) {
            val txt = char.toString()
            val size = FontManager.getSize(font, txt, -1, -1)
            val sizeFirst = GFXx2D.getSizeX(size)
            val sizeSecond = GFXx2D.getSizeY(size)
            h = sizeSecond
            val w = if (equalSpaced) charWidth else sizeFirst
            if (!txt.isBlank2()) {
                val texture = FontManager.getString(font, txt, -1, -1)
                if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                    texture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    val x2 = fx + (w - sizeFirst) / 2
                    shader.v2(
                        "pos",
                        (x2 - GFX.windowX).toFloat() / GFX.windowWidth,
                        1f - (y - GFX.windowY).toFloat() / GFX.windowHeight
                    )
                    shader.v2("size", sizeFirst.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
                    GFX.flat01.draw(shader)
                    GFX.check()
                } else {
                    LOGGER.warn(
                        "Texture for '$txt' is ${
                            if (texture == null) "null"
                            else if (texture is Texture2D && texture.isDestroyed) "destroyed"
                            else "not created"
                        }, $texture"
                    )
                }
            }
            fx += w
        }

        GFX.loadTexturesSync.pop()

        return GFXx2D.getSize(fx - x, h)

    }

    fun drawText(
        x: Int, y: Int,
        font: Font, text: String,
        color: Int, backgroundColor: Int,
        widthLimit: Int,
        heightLimit: Int,
        alignment: AxisAlignment = AxisAlignment.MIN
    ): Int {

        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)

        GFX.check()
        val tex0 = FontManager.getString(font, text, widthLimit, heightLimit)

        val charByChar = (tex0 == null || tex0 !is Texture2D || !tex0.isCreated || tex0.isDestroyed) && text.length > 1
        return if (charByChar) {
            drawTextCharByChar(x, y, font, text, color, backgroundColor, widthLimit, heightLimit, alignment, false)
        } else drawText(x, y, color, backgroundColor, tex0!!, alignment)

    }

    fun drawText(
        x: Int, y: Int,
        color: Int, backgroundColor: Int,
        texture: ITexture2D,
        alignment: AxisAlignment
    ): Int {
        val w = texture.w
        val h = texture.h
        // done if pixel is on the border of the drawn rectangle, make it grayscale, so we see no color seams
        if (texture !is Texture2D || texture.isCreated) {
            GFX.check()
            texture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            GFX.check()
            val shader = ShaderLib.subpixelCorrectTextShader.value
            shader.use()
            val xWithOffset = x - when (alignment) {
                AxisAlignment.MIN, AxisAlignment.FILL -> 0
                AxisAlignment.CENTER -> w / 2
                AxisAlignment.MAX -> w
            }
            val windowWidth = GFX.windowWidth.toFloat()
            val windowHeight = GFX.windowHeight.toFloat()
            shader.v2(
                "pos",
                (xWithOffset - GFX.windowX) / windowWidth,
                1f - (y - GFX.windowY) / windowHeight
            )
            shader.v2("size", w / windowWidth, -h / windowHeight)
            shader.v2("windowSize", windowWidth, windowHeight)
            shader.v4("textColor", color)
            shader.v4("backgroundColor", backgroundColor)
            GFX.flat01.draw(shader)
            GFX.check()
        }
        return GFXx2D.getSize(w, h)
    }

    fun drawText(
        x: Int, y: Int,
        font: Font, key: TextCacheKey,
        color: Int, backgroundColor: Int,
        alignmentX: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()

        val tex0 = FontManager.getString(key)
        val charByChar = tex0 == null || tex0 !is Texture2D || !tex0.isCreated
        if (charByChar) {
            return drawTextCharByChar(
                x,
                y,
                font,
                key.text,
                color,
                backgroundColor,
                key.widthLimit,
                key.heightLimit,
                alignmentX,
                false
            )
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, font.sizeInt)
        return drawText(x, y, color, backgroundColor, texture, alignmentX)

    }

    // minimalistic function only using key, coordinates, colors, and whether it's centered horizontally
    fun drawText(
        x: Int, y: Int,
        key: TextCacheKey,
        color: Int, backgroundColor: Int,
        alignment: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()

        val tex0 = FontManager.getString(key)
        val charByChar = tex0 == null || tex0 !is Texture2D || !tex0.isCreated
        if (charByChar) {
            return drawTextCharByChar(
                x, y, key.createFont(), key.text, color,
                backgroundColor, key.widthLimit, key.heightLimit, alignment, false
            )
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, FontManager.getAvgFontSize(key.fontSizeIndex()).roundToInt())
        return drawText(x, y, color, backgroundColor, texture, alignment)

    }

    fun getTextSizeX(font: Font, text: String, widthLimit: Int, heightLimit: Int) =
        GFXx2D.getSizeX(getTextSize(font, text, widthLimit, heightLimit))

    fun getTextSizeY(font: Font, text: String, widthLimit: Int, heightLimit: Int) =
        GFXx2D.getSizeY(getTextSize(font, text, widthLimit, heightLimit))

    fun getTextSize(font: Font, text: String, widthLimit: Int, heightLimit: Int) =
        FontManager.getSize(font, text, widthLimit, heightLimit)

    fun getTextSize(key: TextCacheKey) =
        FontManager.getSize(key)

}