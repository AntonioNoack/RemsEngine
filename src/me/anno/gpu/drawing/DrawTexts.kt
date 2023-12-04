package me.anno.gpu.drawing

import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.fonts.TextGroup
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawCurves.putRGBA
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.drawing.GFXx2D.posSizeDraw
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.*
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths
import me.anno.ui.base.Font
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL45C
import kotlin.math.min
import kotlin.math.roundToInt

object DrawTexts {

    private val LOGGER = LogManager.getLogger(DrawTexts::class)

    val simpleChars = Array(126 + 1 - 33) { (it + 33).toChar().toString() }

    val monospaceFont by lazy {
        val size = DefaultConfig.style.getSize("fontSize", 12)
        val fonts = FontManager.fontSet
        val bold = false
        val italic = false
        val fontName = when {
            "Consolas" in fonts -> "Consolas" // best case
            "Courier New" in fonts -> "Courier New" // second best case
            else -> fonts.firstOrNull { it.contains("mono", true) } ?: fonts.firstOrNull() ?: "Courier New"
        }
        Font(fontName, size, bold, italic)
    }

    val simpleBatch = object : Batch(
        "simpleTextBatch", flat01, listOf(
            Attribute("instData", 3),
            Attribute("color0", AttributeType.UINT8_NORM, 4),
            Attribute("color1", AttributeType.UINT8_NORM, 4),
        ), 4096
    ) {
        override fun bindShader() = ShaderLib.subpixelCorrectTextShader[1].value
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawSimpleTextCharByChar(
        x, y, padding, text, FrameTimings.textColor,
        FrameTimings.backgroundColor or black,
        alignX, alignY
    )

    fun startSimpleBatch(): Int {
        val font = monospaceFont
        val x = pushBetterBlending(false)
        val shader = chooseShader(-1, -1, 1)
        val texture = FontManager.getASCIITexture(font)
        texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
        val v = if (shader is Shader) {
            val v = simpleBatch.start()
            posSize(shader, 0f, 0f, texture.width.toFloat(), texture.height.toFloat())
            v
        } else 0
        return if (x) v else v.inv()
    }

    fun finishSimpleBatch(v: Int) {
        simpleBatch.finish(if (v < 0) v.inv() else v)
        popBetterBlending(v < 0)
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: CharSequence,
        textColor: Int = FrameTimings.textColor,
        backgroundColor: Int = FrameTimings.backgroundColor or black,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
        batched: Boolean = false
    ): Int {

        val font = monospaceFont
        val charWidth = font.sampleWidth
        val size = text.length
        val width = charWidth * size
        val height = font.sampleHeight

        val dx0 = getOffset(width, alignX) - padding
        val dy0 = getOffset(height, alignY) - padding

        if (backgroundColor.a() != 0) DrawRectangles.drawRect(
            x + dx0, y + dy0,
            charWidth * text.length + 2 * padding, font.sizeInt + 2 * padding,
            backgroundColor
        )

        val background = backgroundColor and 0xffffff

        val texture = FontManager.getASCIITexture(font)
        var v = 0
        val shader = if (!batched) {
            val shader = chooseShader(textColor, background, 1)
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            if (shader is Shader) {
                v = simpleBatch.start()
                posSize(shader, 0f, 0f, texture.width.toFloat(), texture.height.toFloat())
            }
            shader
        } else null

        val y2 = y + dy0 + padding - 1
        var x2 = x + dx0 + padding + (charWidth - texture.width) / 2

        if (shader !is ComputeShader) {
            val posY = 1f - (y2 - GFX.viewportY).toFloat() / GFX.viewportHeight
            var x2f = (x2 - GFX.viewportX).toFloat() / GFX.viewportWidth
            val dxf = charWidth.toFloat() / GFX.viewportWidth
            for (i in text.indices) {
                val char = text[i]
                val code = char.code - 33
                if (code in simpleChars.indices) {
                    simpleBatch.data.putFloat(x2f).putFloat(posY).putFloat(code.toFloat())
                        .putRGBA(textColor).putRGBA(background)
                    simpleBatch.next()
                }
                x2f += dxf
            }
        } else {
            for (i in text.indices) {
                val char = text[i]
                val code = char.code - 33
                if (code in simpleChars.indices) {
                    shader.v1f("uvZ", code.toFloat())
                    posSizeDraw(shader, x2, y2, texture.width, texture.height, 1)
                }
                x2 += charWidth
            }
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
        }

        if (shader is Shader) {
            simpleBatch.finish(v)
        }

        return width
    }

    // slightly buggy (missing barriers?), but allows for correct rendering of text against any background with correct subpixel rendering
    private var enableComputeRendering = false

    fun pushBetterBlending(enabled: Boolean): Boolean {
        val pbb = enableComputeRendering
        enableComputeRendering = enabled
        return pbb
    }

    fun popBetterBlending(pbb: Boolean) {
        enableComputeRendering = pbb
    }

    fun canUseComputeShader(): Boolean {
        if (!enableComputeRendering) return false
        if (!GFX.supportsComputeShaders) return false
        if (GFXState.currentBuffer == NullFramebuffer) return false
        return transform.isIdentity()
    }

    fun drawTextCharByChar(
        x: Int, y: Int,
        font: Font,
        text: CharSequence,
        textColor: Int,
        backgroundColor: Int,
        widthLimit: Int,
        heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
        equalSpaced: Boolean
    ): Int {

        if ('\n' in text) {
            var sizeX = 0
            val split = text.split('\n')
            val lineOffset = font.sizeInt * 3 / 2
            for (index in split.indices) {
                val s = split[index]
                val size = drawTextCharByChar(
                    x, y + index * lineOffset, font, s,
                    textColor, backgroundColor,
                    widthLimit, heightLimit, alignX, alignY, equalSpaced
                )
                sizeX = Maths.max(GFXx2D.getSizeX(size), sizeX)
            }
            return GFXx2D.getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        if (text.isEmpty())
            return GFXx2D.getSize(0, font.sizeInt)

        val shader = chooseShader(textColor, backgroundColor)
        GFX.check()

        if (equalSpaced) {

            val charWidth = font.sampleWidth
            val textWidth = charWidth * text.length

            val dx = getOffset(textWidth, alignX)
            val dy = getOffset(font.sampleHeight, alignY)
            val y2 = y + dy - 1

            // todo respect width limit

            GFX.loadTexturesSync.push(true)

            var fx = x + dx
            for (char in text.codePoints()) {
                if (!Character.isWhitespace(char)) {
                    val txt = char.joinChars().toString()
                    val texture = FontManager.getTexture(font, txt, -1, -1)
                    if (texture != null) draw(shader, texture, fx + (charWidth - texture.width) / 2, y2, txt, false)
                }
                fx += charWidth
            }
            if (shader is ComputeShader) {
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            }

            GFX.loadTexturesSync.pop()

            val size = FontManager.getSize(font, text, -1, -1)
            return GFXx2D.getSize(fx - (x + dx), GFXx2D.getSizeY(size))
        } else {

            val font2 = FontManager.getFont(font)
            val group = TextGroup(font2, text, 0.0)

            val textWidth = group.offsets.last().toFloat()

            val dxi = getOffset(textWidth.roundToInt(), alignX)
            val dyi = getOffset(font.sampleHeight, alignY)

            GFX.loadTexturesSync.push(true)

            var index = 0
            val offsets = group.offsets
            val y2 = y + dyi
            for (char in text.codePoints()) {
                if (!Character.isWhitespace(char)) {
                    val txt = char.joinChars().toString()
                    val o0 = offsets[index++].toInt()
                    val o1 = offsets[index].toInt()
                    val fx = x + dxi + o0
                    val w = o1 - o0
                    val texture = FontManager.getTexture(font, txt, -1, -1)
                    if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                        texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
                        val x2 = fx + (w - texture.width).shr(1)
                        draw(shader, texture, x2, y2, txt, false)
                    }
                } else index++
            }
            if (shader is ComputeShader) {
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            }

            GFX.loadTexturesSync.pop()

            val size = FontManager.getSize(font, text, -1, -1)
            return GFXx2D.getSize(textWidth.roundToInt(), GFXx2D.getSizeY(size))
        }
    }

    private fun chooseShader(textColor: Int, backgroundColor: Int, instanced: Int = 0): OpenGLShader {
        GFX.check()
        val cuc = canUseComputeShader() && min(textColor.a(), backgroundColor.a()) < 255
        val shader = if (cuc && !ShaderLib.subpixelCorrectTextShader2[instanced].failedCompilation) {
            val shader = ShaderLib.subpixelCorrectTextShader2[instanced]
            try {
                shader.use()
                shader.bindTexture(
                    1, GFXState.currentBuffer.getTexture0() as Texture2D,
                    ComputeTextureMode.READ_WRITE
                )
                shader
            } catch (e: Exception) {
                shader.failedCompilation = true
                LOGGER.warn("Failed to compile subpixel blending shader", e)
                ShaderLib.subpixelCorrectTextShader[instanced].value
            }
        } else ShaderLib.subpixelCorrectTextShader[instanced].value
        shader.use()
        shader.v4f("textColor", textColor)
        shader.v4f("backgroundColor", backgroundColor)
        val windowWidth = GFX.viewportWidth.toFloat()
        val windowHeight = GFX.viewportHeight.toFloat()
        shader.v2f("windowSize", windowWidth, windowHeight)
        return shader
    }

    fun drawTextChar(
        x: Int, y: Int,
        font: Font,
        key: TextCacheKey,
        textColor: Int,
        backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
        equalSpaced: Boolean
    ): Int {

        if (key.text.isEmpty())
            return GFXx2D.getSize(0, font.sizeInt)
        if (key.text.isBlank2())
            return FontManager.getSize(key)

        GFX.check()

        val shader = chooseShader(textColor, backgroundColor)
        GFX.check()

        GFX.loadTexturesSync.push(true)

        val charWidth = if (equalSpaced) {

            val wx = font.sampleWidth

            val dx = getOffset(wx, alignX)
            val dy = getOffset(font.sampleHeight, alignY)
            val y2 = y + dy - 1

            val txt = key.text.toString()

            val texture = FontManager.getTexture(key)
            if (texture != null) {
                draw(shader, texture, x + dx + (wx - texture.width).shr(1), y2, txt, true)
            }

            wx
        } else {

            val font2 = FontManager.getFont(font)
            val text = key.text
            val offsets = TextGroup(font2, text, 0.0).offsets

            val textWidth = offsets.last().toFloat()

            val wx = textWidth.roundToInt()
            val dxi = getOffset(wx, alignX)
            val dyi = getOffset(font.sampleHeight, alignY)

            val y2 = y + dyi

            val o0 = offsets[0].toInt()
            val o1 = offsets[1].toInt()
            val fx = x + dxi + o0
            val w = o1 - o0

            val texture = FontManager.getTexture(key)
            if (texture != null) {
                draw(shader, texture, fx + (w - texture.width).shr(1), y2, text, true)
            }

            wx
        }

        GFX.loadTexturesSync.pop()

        val size = FontManager.getSize(key)
        return GFXx2D.getSize(charWidth, GFXx2D.getSizeY(size))
    }

    private fun draw(
        shader: OpenGLShader, texture: ITexture2D?,
        x2: Int, y2: Int, txt: CharSequence, barrier: Boolean
    ) {
        if (texture != null && (texture !is Texture2D || texture.isCreated)) {
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            shader.use()
            if (shader is Shader) {
                posSize(shader, x2, y2, texture.width, texture.height)
                flat01.draw(shader)
            } else {
                shader as ComputeShader
                posSizeDraw(shader, x2, y2, texture.width, texture.height, 1)
                if (barrier) glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            }
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
        } else drawText(x, y, color, backgroundColor, tex0 ?: whiteTexture, alignX, alignY)
    }

    fun drawText(
        x: Int, y: Int,
        textColor: Int, backgroundColor: Int,
        texture: ITexture2D,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {
        val w = texture.width
        val h = texture.height
        // done if pixel is on the border of the drawn rectangle, make it grayscale, so we see no color seams
        if (texture !is Texture2D || texture.isCreated) {
            GFX.check()
            GFX.check()
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            val x2 = x + getOffset(w, alignX)
            val y2 = y + getOffset(h, alignY)
            val shader = chooseShader(textColor, backgroundColor)
            if (shader is ComputeShader) {
                posSizeDraw(shader, x2, y2, w, h, 1)
                GL45C.glMemoryBarrier(GL45C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            } else {
                shader as Shader
                posSize(shader, x2, y2, w, h)
                flat01.draw(shader)
            }
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
        alignY: AxisAlignment = AxisAlignment.MIN,
        equalSpaced: Boolean = false
    ): Int {
        GFX.check()
        if (equalSpaced) {
            return drawTextCharByChar(
                x, y, font, key.text,
                color, backgroundColor,
                key.widthLimit,
                key.heightLimit,
                alignX, alignY,
                true
            )
        } else {
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

        if (key.text.length < 2) {
            val font = key.createFont()
            return drawTextChar(
                x, y, font, key, color, backgroundColor,
                alignX, alignY, font == monospaceFont
            )
        }

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

    @Suppress("unused")
    fun getTextSizeY(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int) =
        GFXx2D.getSizeY(getTextSize(font, text, widthLimit, heightLimit))

    fun getTextSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int) =
        FontManager.getSize(font, text, widthLimit, heightLimit)

    fun getTextSize(key: TextCacheKey) =
        FontManager.getSize(key)
}