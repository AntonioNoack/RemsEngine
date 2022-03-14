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
import me.anno.maths.Maths
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.FrameTimes
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.roundToInt

object DrawTexts {

    private val LOGGER = LogManager.getLogger(DrawTexts::class)

    val simpleChars = Array('z'.code + 1) { it.toChar().toString() }

    val monospaceFont by lazy {
        val size = DefaultConfig.style.getSize("fontSize", 12)
        Font("Consolas", size, false, false)
    }

    val monospaceKeys by lazy {
        Array(simpleChars.size) {
            FontManager.getTextCacheKey(monospaceFont, simpleChars[it], -1, -1)
        }
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: CharArray,
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ) {
        val font = monospaceFont
        val keys = monospaceKeys
        val charWidth = font.sampleWidth
        val size = text.size
        val width = charWidth * size
        val height = font.sampleHeight
        val dx = getOffset(width, alignX)
        val dy = getOffset(height, alignY)
        DrawRectangles.drawRect(
            x + dx, y + dy,
            charWidth * text.size + 2 * padding, font.sizeInt + 2 * padding,
            backgroundColor
        )
        var x1 = x + dx + padding
        val y1 = y + dy + padding
        for (i in text.indices) {
            val char = text[i]
            val charInt = char.code
            if (charInt < simpleChars.size) {
                val key = keys[charInt] ?: continue
                drawText(
                    x1, y1,
                    font, key, textColor, backgroundColor.and(0xffffff)
                )
            }
            x1 += charWidth
        }
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: String,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawSimpleTextCharByChar(
        x, y, padding, text, FrameTimes.textColor,
        FrameTimes.backgroundColor, alignX, alignY
    )

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: String,
        textColor: Int = FrameTimes.textColor,
        backgroundColor: Int = FrameTimes.backgroundColor,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int {
        GFX.check()
        val font = monospaceFont
        val keys = monospaceKeys
        val charWidth = font.sampleWidth
        val size = text.length
        val width = charWidth * size
        val height = font.sampleHeight
        val dx = getOffset(width, alignX)
        val dy = getOffset(height, alignY)
        DrawRectangles.drawRect(
            x + dx, y + dy,
            charWidth * text.length + 2 * padding, font.sizeInt + 2 * padding,
            backgroundColor
        )
        for (i in text.indices) {
            val char = text[i]
            val charInt = char.code
            if (charInt < simpleChars.size) {
                val key = keys[charInt] ?: continue
                drawText(
                    x + dx + padding + i * charWidth, y + dy + padding,
                    font, key, textColor, backgroundColor.and(0xffffff)
                )
            }
        }
        return width
    }

    fun drawTextCharByChar(
        x: Int, y: Int,
        font: Font,
        text: CharSequence,
        color: Int,
        backgroundColor: Int,
        widthLimit: Int,
        heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
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
                    widthLimit, heightLimit, alignX, alignY, equalSpaced
                )
                sizeX = Maths.max(GFXx2D.getSizeX(size), sizeX)
            }
            return GFXx2D.getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        val charWidth = if (equalSpaced) getTextSizeX(font, "x", widthLimit, heightLimit) else 0

        val dx = getOffset(charWidth * text.length, alignX)
        val dy = getOffset(font.sampleHeight, alignY)

        // todo width limit...

        val shader = ShaderLib.subpixelCorrectTextShader.value
        shader.use()
        shader.v4f("textColor", color)
        shader.v4f("backgroundColor", backgroundColor)

        GFX.loadTexturesSync.push(true)

        var fx = x + dx
        val fy = 1f - (y + dy - GFX.viewportY).toFloat() / GFX.viewportHeight
        var h = font.sizeInt
        for (char in text) {
            val txt = char.toString()
            val size = FontManager.getSize(font, txt, -1, -1)
            val sizeX = GFXx2D.getSizeX(size)
            val sizeY = GFXx2D.getSizeY(size)
            h = sizeY
            val w = if (equalSpaced) charWidth else sizeX
            if (!txt.isBlank2()) {
                val texture = FontManager.getTexture(font, txt, -1, -1)
                if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                    texture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    val x2 = fx + (w - sizeX) / 2
                    shader.v2f("pos", (x2 - GFX.viewportX).toFloat() / GFX.viewportWidth, fy)
                    shader.v2f("size", sizeX.toFloat() / GFX.viewportWidth, -h.toFloat() / GFX.viewportHeight)
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

        return GFXx2D.getSize(fx - (x + dx), h)

    }

    fun drawText(
        x: Int, y: Int,
        font: Font, text: String,
        color: Int, backgroundColor: Int,
        widthLimit: Int,
        heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {

        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)

        GFX.check()
        val tex0 = FontManager.getTexture(font, text, widthLimit, heightLimit)

        val charByChar = (tex0 == null || tex0 !is Texture2D || !tex0.isCreated || tex0.isDestroyed) && text.length > 1
        return if (charByChar) {
            drawTextCharByChar(x, y, font, text, color, backgroundColor, widthLimit, heightLimit, alignX, alignY, false)
        } else drawText(x, y, color, backgroundColor, tex0!!, alignX, alignY)

    }

    fun drawText(
        x: Int, y: Int,
        color: Int, backgroundColor: Int,
        texture: ITexture2D,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
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
            val x2 = x + getOffset(w, alignX)
            val y2 = y + getOffset(h, alignY)
            val windowWidth = GFX.viewportWidth.toFloat()
            val windowHeight = GFX.viewportHeight.toFloat()
            shader.v2f(
                "pos",
                (x2 - GFX.viewportX) / windowWidth,
                1f - (y2 - GFX.viewportY) / windowHeight
            )
            shader.v2f("size", w / windowWidth, -h / windowHeight)
            shader.v2f("windowSize", windowWidth, windowHeight)
            shader.v4f("textColor", color)
            shader.v4f("backgroundColor", backgroundColor)
            GFX.flat01.draw(shader)
            GFX.check()
        }
        return GFXx2D.getSize(w, h)
    }

    fun getOffset(size: Int, alignment: AxisAlignment): Int {
        return -when (alignment) {
            AxisAlignment.MIN, AxisAlignment.FILL -> 0
            AxisAlignment.CENTER -> size / 2
            AxisAlignment.MAX -> size
        }
    }

    fun drawText(
        x: Int, y: Int,
        font: Font, key: TextCacheKey,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()

        val tex0 = FontManager.getTexture(key)
        val charByChar = tex0 == null || tex0 !is Texture2D || !tex0.isCreated
        if (charByChar) {
            return drawTextCharByChar(
                x, y, font, key.text,
                color, backgroundColor,
                key.widthLimit,
                key.heightLimit,
                alignX, alignY,
                false
            )
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, font.sizeInt)
        return drawText(x, y, color, backgroundColor, texture, alignX, alignY)

    }

    // minimalistic function only using key, coordinates, colors, and whether it's centered horizontally
    fun drawText(
        x: Int, y: Int,
        key: TextCacheKey,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {

        GFX.check()

        val tex0 = FontManager.getTexture(key)
        val charByChar = tex0 == null || tex0 !is Texture2D || !tex0.isCreated
        if (charByChar) {
            return drawTextCharByChar(
                x, y, key.createFont(), key.text, color,
                backgroundColor, key.widthLimit, key.heightLimit, alignX, alignY, false
            )
        }

        val texture = tex0 ?: return GFXx2D.getSize(0, FontManager.getAvgFontSize(key.fontSizeIndex()).roundToInt())
        return drawText(x, y, color, backgroundColor, texture, alignX, alignY)

    }

    fun getTextSizeX(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int) =
        GFXx2D.getSizeX(getTextSize(font, text, widthLimit, heightLimit))

    fun getTextSizeY(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int) =
        GFXx2D.getSizeY(getTextSize(font, text, widthLimit, heightLimit))

    fun getTextSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int) =
        FontManager.getSize(font, text, widthLimit, heightLimit)

    fun getTextSize(key: TextCacheKey) =
        FontManager.getSize(key)

}