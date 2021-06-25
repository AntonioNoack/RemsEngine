package me.anno.gpu.drawing

import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.FrameTimes
import me.anno.utils.LOGGER
import me.anno.utils.Maths
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.roundToInt

object DrawTexts {

    val simpleChars = Array('z'.code + 1) { it.toChar().toString() }
    var monospaceFont = lazy { Font("Consolas", DefaultConfig.style.getSize("fontSize", 12), false, false) }
    val monospaceKeys =
        lazy { Array(simpleChars.size) { FontManager.getTextCacheKey(monospaceFont.value, simpleChars[it], -1) } }

    fun drawSimpleTextCharByChar(
        x0: Int, y0: Int,
        padding: Int,
        text: CharArray,
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignment: AxisAlignment = AxisAlignment.MIN
    ) {
        val font = monospaceFont.value
        val keys = monospaceKeys.value
        val charWidth = font.sampleWidth
        val size = text.size
        val width = charWidth * size
        val offset = -when (alignment) {
            AxisAlignment.MIN -> 0
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
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignment: AxisAlignment = AxisAlignment.MIN
    ) {

        val font = monospaceFont.value
        val keys = monospaceKeys.value
        val charWidth = font.sampleWidth
        val size = text.length
        val width = charWidth * size
        val offset = -when (alignment) {
            AxisAlignment.MIN -> 0
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
                    widthLimit, alignment, equalSpaced
                )
                sizeX = Maths.max(GFXx2D.getSizeX(size), sizeX)
            }
            return GFXx2D.getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        val charWidth = if (equalSpaced) getTextSizeX(font, "x", widthLimit) else 0

        val offset = when (alignment) {
            AxisAlignment.MIN -> 0
            AxisAlignment.CENTER -> -charWidth * text.length / 2
            AxisAlignment.MAX -> -charWidth * text.length
        }

        if (offset != 0) {
            drawTextCharByChar(
                x + offset, y,
                font, text, color, backgroundColor,
                widthLimit, AxisAlignment.MIN, equalSpaced
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
            val size = FontManager.getSize(font, txt, -1)
            val sizeFirst = GFXx2D.getSizeX(size)
            val sizeSecond = GFXx2D.getSizeY(size)
            h = sizeSecond
            val w = if (equalSpaced) charWidth else sizeFirst
            if (!txt.isBlank2()) {
                val texture = FontManager.getString(font, txt, -1)
                if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                    texture.bind(GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    val x2 = fx + (w - sizeFirst) / 2
                    shader.v2(
                        "pos",
                        (x2 - GFX.windowX).toFloat() / GFX.windowWidth,
                        1f - (y - GFX.windowY).toFloat() / GFX.windowHeight
                    )
                    shader.v2("size", sizeFirst.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
                    GFX.flat01.draw(shader)
                    GFX.check()
                } else LOGGER.warn("Texture for $txt is null")
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
        alignment: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()
        val tex0 = FontManager.getString(font, text, widthLimit)
        val charByChar = (tex0 == null || tex0 !is Texture2D || !tex0.isCreated) && text.length > 1
        if (charByChar) {
            return drawTextCharByChar(x, y, font, text, color, backgroundColor, widthLimit, alignment, false)
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, font.sizeInt)
        return drawText(x, y, color, backgroundColor, texture, alignment)

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
            texture.bind(GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            val shader = ShaderLib.subpixelCorrectTextShader.value
            shader.use()
            val xWithOffset = x - when (alignment) {
                AxisAlignment.MIN -> 0
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
        alignment: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()

        val tex0 = FontManager.getString(key)
        val charByChar = tex0 == null || tex0 !is Texture2D || !tex0.isCreated
        if (charByChar) {
            return drawTextCharByChar(x, y, font, key.text, color, backgroundColor, key.widthLimit, alignment, false)
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, font.sizeInt)
        return drawText(x, y, color, backgroundColor, texture, alignment)

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
                backgroundColor, key.widthLimit, alignment, false
            )
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, FontManager.getAvgFontSize(key.fontSizeIndex()).roundToInt())
        return drawText(x, y, color, backgroundColor, texture, alignment)

    }

    fun getTextSizeX(font: Font, text: String, widthLimit: Int) =
        GFXx2D.getSizeX(getTextSize(font, text, widthLimit))

    fun getTextSizeY(font: Font, text: String, widthLimit: Int) =
        GFXx2D.getSizeY(getTextSize(font, text, widthLimit))

    fun getTextSize(font: Font, text: String, widthLimit: Int) =
        FontManager.getSize(font, text, widthLimit)

}