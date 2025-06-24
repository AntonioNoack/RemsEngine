package me.anno.gpu.drawing

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.TextGroup
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawCurves.putRGBA
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.drawing.GFXx2D.posSizeDraw
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.maths.Maths
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import kotlin.math.min

object DrawTexts {

    private val LOGGER = LogManager.getLogger(DrawTexts::class)

    val simpleChars = createArrayList(126 + 1 - 33) { (it + 33).toChar().toString() }

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

    private val simpleBatch = object : Batch(
        "simpleTextBatch", flat01, bind(
            Attribute("instData", 3),
            Attribute("color0", AttributeType.UINT8_NORM, 4),
            Attribute("color1", AttributeType.UINT8_NORM, 4),
        ), 4096
    ) {
        override fun bindShader(): Shader {
            val shader = ShaderLib.subpixelCorrectTextGraphicsShader[1].value
            shader.use()
            return shader
        }
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int,
        padding: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawSimpleTextCharByChar(
        x, y, padding, text, FrameTimings.textColor,
        FrameTimings.background.color or black,
        alignX, alignY
    )

    fun startSimpleBatch(): Int {
        val font = monospaceFont
        val x = pushBetterBlending(false)
        val shader = chooseShader(-1, -1, 1)
        val texture = FontManager.getASCIITexture(font)
        texture.bindTrulyNearest(0)
        val batch = if (shader is Shader) {
            val batch = simpleBatch.start()
            if (batch == 0) {
                shader.use() // just in case
                posSize(shader, 0f, 0f, texture.width.toFloat(), texture.height.toFloat())
            }
            batch
        } else 0
        return if (x) batch else batch.inv()
    }

    fun finishSimpleBatch(batch: Int) {
        simpleBatch.finish(if (batch < 0) batch.inv() else batch)
        popBetterBlending(batch >= 0)
    }

    fun drawSimpleTextCharByChar(
        x: Int, y: Int, padding: Int, text: CharSequence,
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

        if (backgroundColor.a() != 0) {
            DrawRectangles.drawRect(
                x + dx0, y + dy0,
                charWidth * text.length + 2 * padding, font.sizeInt + 2 * padding,
                backgroundColor
            )
        }

        val background = backgroundColor and 0xffffff

        val texture = FontManager.getASCIITexture(font)
        var v = 1
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

        if (shader is ComputeShader) {
            fun drawChar(i: Int) {
                val char = text[i]
                val code = char.code - 33
                if (code in simpleChars.indices) {
                    shader.v1f("uvZ", code.toFloat())
                    posSizeDraw(shader, x2, y2, texture.width, texture.height, 1)
                }
            }
            forLoop(0, text.length, 2) { i ->
                drawChar(i)
                x2 += charWidth * 2
            }
            // must be called on every char overlap, or we get flickering
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            x2 = x + dx0 + padding + (charWidth - texture.width) / 2 + charWidth
            forLoop(1, text.length, 2) { i ->
                drawChar(i)
                x2 += charWidth * 2
            }
            if (text.length > 1) glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            // x2 = x + dx0 + padding + (charWidth - texture.width) / 2 + charWidth * text.length
        } else {
            val posY = 1f - (y2 - GFX.viewportY).toFloat() / GFX.viewportHeight
            var x2f = (x2 - GFX.viewportX).toFloat() / GFX.viewportWidth
            val dxf = charWidth.toFloat() / GFX.viewportWidth
            for (i in text.indices) {
                val char = text[i]
                val code = char.code - 33
                if (code in simpleChars.indices) {
                    simpleBatch.data
                        .putFloat(x2f).putFloat(posY).putFloat(code.toFloat())
                        .putRGBA(textColor).putRGBA(background)
                    simpleBatch.next()
                }
                x2f += dxf
            }
            simpleBatch.finish(v)
        }

        return width
    }

    var enableComputeRendering = false
        private set

    fun pushBetterBlending(enabled: Boolean): Boolean {
        val pbb = enableComputeRendering
        enableComputeRendering = enabled
        return pbb
    }

    fun popBetterBlending(pbb: Boolean) {
        enableComputeRendering = pbb
    }

    var enableTrueBlending = false
        private set

    fun pushTrueBlending(enabled: Boolean): Boolean {
        val pbb = enableTrueBlending
        enableTrueBlending = enabled
        return pbb
    }

    fun popTrueBlending(pbb: Boolean) {
        enableTrueBlending = pbb
    }

    fun canUseComputeShader(): Boolean {
        if (!enableComputeRendering) return false
        if (!GFX.supportsComputeShaders) return false
        if (GFXState.currentBuffer == NullFramebuffer) return false
        return transform.isIdentity()
    }

    fun getTextSizeCharByChar(font: Font, text: CharSequence, equalSpaced: Boolean): Int {

        if ('\n' in text) {
            var sizeX = 0
            val split = text.split('\n')
            val lineOffset = font.sizeInt * 3 / 2
            for (index in split.indices) {
                val size = getTextSizeCharByChar(font, split[index], equalSpaced)
                sizeX = Maths.max(getSizeX(size), sizeX)
            }
            return getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        if (text.isEmpty())
            return getSize(0, font.sizeInt)

        return if (equalSpaced) {
            val charWidth = font.sampleWidth
            val textWidth = charWidth * text.length
            getSize(textWidth, font.sizeInt)
        } else {
            val group = TextGroup(font, text, 0.0)
            val textWidth = group.offsets.last().roundToIntOr()
            getSize(textWidth, font.sizeInt)
        }
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
                val size = drawTextCharByChar(
                    x, y + index * lineOffset, font, split[index],
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

            fun getTexture(char: Int): ITexture2D? {
                return if (char > 0xffff || !char.toChar().isWhitespace()) {
                    val txt = char.joinChars().toString()
                    FontManager.getTexture(font, txt, -1, -1).waitFor()
                } else null
            }

            var fx = x + dx - charWidth // -charWidth for 0th iteration
            val cp = text.codepoints().iterator()

            // to do: with more context like 4 or 5 characters, we could improve the layout even more
            var tex0: ITexture2D?
            var tex1: ITexture2D? = null
            var tex2: ITexture2D? = null

            fun drawChar(texture: ITexture2D?) {
                tex0 = tex1
                tex1 = tex2
                tex2 = texture
                val texI = tex1
                if (texI != null) {
                    var x2 = fx * 4 + (charWidth - texI.width) * 2
                    if (tex0 != null && tex2 != null) {
                        x2 += (tex0!!.width - tex2!!.width)
                    }
                    // +2 for rounding
                    draw(shader, texI, (x2 + 2) shr 2, y2, "?", false)
                }
                fx += charWidth
            }

            // extra iteration at the end
            while (cp.hasNext()) {
                drawChar(getTexture(cp.nextInt()))
            }
            drawChar(null)

            if (shader is ComputeShader) {
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            }

            GFX.loadTexturesSync.pop()

            return GFXx2D.getSize(fx - (x + dx), font.sizeInt)
        } else {

            val group = TextGroup(font, text, 0.0)

            val textWidth = group.offsets.last().roundToIntOr()

            val dxi = getOffset(textWidth, alignX)
            val dyi = getOffset(font.sampleHeight, alignY)

            GFX.loadTexturesSync.push(true)

            var index = 0
            val offsets = group.offsets
            val y2 = y + dyi
            for (char in text.codepoints()) {
                if (char > 0xffff || !char.toChar().isWhitespace()) {
                    val txt = char.joinChars().toString()
                    val o0 = offsets[index++].toInt()
                    val o1 = offsets[index].toInt()
                    val fx = x + dxi + o0
                    val w = o1 - o0
                    val texture = FontManager.getTexture(font, txt, -1, -1)
                        .waitFor()
                    if (texture != null && texture.wasCreated) {
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

            return GFXx2D.getSize(textWidth, font.sizeInt)
        }
    }

    var disableSubpixelRendering = false
    private fun chooseShader(textColor: Int, backgroundColor: Int, instanced: Int = 0): GPUShader {
        GFX.check()
        val cuc = canUseComputeShader() && min(textColor.a(), backgroundColor.a()) < 255
        val shader = if (cuc && !ShaderLib.subpixelCorrectTextComputeShader[instanced].failedCompilation) {
            val shader = ShaderLib.subpixelCorrectTextComputeShader[instanced]
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
                ShaderLib.subpixelCorrectTextGraphicsShader[instanced].value
            }
        } else ShaderLib.subpixelCorrectTextGraphicsShader[instanced].value
        shader.use()
        GFX.check()
        shader.v4f("textColor", textColor)
        shader.v4f("backgroundColor", backgroundColor)
        shader.v1b("disableSubpixelRendering", disableSubpixelRendering)
        shader.v1b("enableTrueBlending", enableTrueBlending)
        GFX.check()
        val windowWidth = GFX.viewportWidth.toFloat()
        val windowHeight = GFX.viewportHeight.toFloat()
        shader.v2f("windowSize", windowWidth, windowHeight)
        GFX.check()
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

        if (key.text.isEmpty()) {
            return GFXx2D.getSize(0, font.sizeInt)
        }

        if (key.text.isBlank2()) {
            return FontManager.getSize(key).waitFor()
                ?: GFXx2D.getSize(0, font.sizeInt)
        }

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

            val texture = FontManager.getTexture(key).waitFor()
            if (texture != null && texture.isCreated()) {
                draw(shader, texture, x + dx + (wx - texture.width).shr(1), y2, txt, true)
            }

            wx
        } else {

            val text = key.text
            val offsets = TextGroup(font, text, 0.0).offsets

            val textWidth = offsets.last()

            val wx = textWidth.roundToIntOr()
            val dxi = getOffset(wx, alignX)
            val dyi = getOffset(font.sampleHeight, alignY)

            val y2 = y + dyi

            val o0 = offsets[0].toInt()
            val o1 = offsets[1].toInt()
            val fx = x + dxi + o0
            val w = o1 - o0

            val texture = FontManager.getTexture(key).waitFor()
            if (texture != null && texture.isCreated()) {
                draw(shader, texture, fx + (w - texture.width).shr(1), y2, text, true)
            }

            wx
        }

        GFX.loadTexturesSync.pop()

        return GFXx2D.getSize(charWidth, font.sizeInt)
    }

    private fun draw(
        shader: GPUShader, texture: ITexture2D?,
        x2: Int, y2: Int, txt: CharSequence, barrier: Boolean
    ) {
        if (texture != null && texture.isCreated()) {
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            shader.use()
            when (shader) {
                is Shader -> {
                    posSize(shader, x2, y2, texture.width, texture.height, true)
                    flat01.draw(shader)
                }
                is ComputeShader -> {
                    posSizeDraw(shader, x2, y2, texture.width, texture.height, 1)
                    if (barrier) glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
                }
            }
        } else {
            LOGGER.warn(
                "Texture for '$txt' is ${
                    if (texture == null) "null"
                    else if (texture.isDestroyed) "destroyed"
                    else "not created"
                }, $texture"
            )
        }
    }

    fun drawText(
        x: Int, y: Int,
        font: Font, text: String,
        color: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {

        if (text.isEmpty()) {
            return GFXx2D.getSize(0, font.sizeInt)
        }

        GFX.check()

        val async = !GFX.loadTexturesSync.peek()
        val tex0 = FontManager.getTexture(font, text, widthLimit, heightLimit)
            .waitFor(async)

        val charByChar = (tex0 == null || !tex0.isCreated()) && text.length > 1
        return if (charByChar) {
            drawTextCharByChar(x, y, font, text, color, backgroundColor, widthLimit, heightLimit, alignX, alignY, false)
        } else drawText(x, y, color, backgroundColor, tex0 ?: blackTexture, alignX, alignY)
    }

    /**
     * aligns and draws the text; returns whether drawing failed
     *
     * todo use this everywhere, maybe remove synchronous text drawing everywhere
     * */
    fun drawTextOrFail(
        x: Int, y: Int,
        font: Font, text: String,
        color: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Boolean {
        if (text.isEmpty()) return false
        val tex0 = FontManager.getTexture(font, text, widthLimit, heightLimit).value
        return if (tex0 != null && tex0.isCreated()) {
            drawText(x, y, color, backgroundColor, tex0, alignX, alignY)
            false
        } else {
            true
        }
    }

    private fun drawText(
        x: Int, y: Int,
        textColor: Int, backgroundColor: Int,
        texture: ITexture2D,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int {
        val w = texture.width
        val h = texture.height
        // done if pixel is on the border of the drawn rectangle, make it grayscale, so we see no color seams
        if (texture.isCreated()) {
            GFX.check()
            GFX.check()
            texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
            val x2 = x + getOffset(w, alignX)
            val y2 = y + getOffset(h, alignY)
            val shader = chooseShader(textColor, backgroundColor)
            if (shader is ComputeShader) {
                posSizeDraw(shader, x2, y2, w, h, 1)
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            } else {
                shader as Shader
                posSize(shader, x2, y2, w, h, true)
                flat01.draw(shader)
            }
            GFX.check()
        }
        return GFXx2D.getSize(w, h)
    }

    fun getOffset(size: Int, alignment: AxisAlignment): Int {
        return alignment.getOffset(0, size - 1)
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
            val async = !GFX.loadTexturesSync.peek()
            val texture = FontManager.getTexture(key)
                .waitFor(async)
            if (texture == null || !texture.isCreated()) { // char by char
                return drawTextCharByChar(
                    x, y, font, key.text,
                    color, backgroundColor,
                    key.widthLimit,
                    key.heightLimit,
                    alignX, alignY,
                    false
                )
            }

            return drawText(x, y, color, backgroundColor, texture, alignX, alignY)
        }
    }

    fun drawTextOrFail(
        x: Int, y: Int,
        font: Font, key: TextCacheKey,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
        equalSpaced: Boolean = false
    ): Boolean {
        GFX.check()
        if (equalSpaced) {
            drawTextCharByChar(
                x, y, font, key.text,
                color, backgroundColor,
                key.widthLimit,
                key.heightLimit,
                alignX, alignY,
                true
            )
            return false
        } else {
            val texture = FontManager.getTexture(key).value
            return if (texture != null && texture.isCreated()) {
                drawText(x, y, color, backgroundColor, texture, alignX, alignY)
                false
            } else true
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

        val async = !GFX.loadTexturesSync.peek()
        val texture = FontManager.getTexture(key)
            .waitFor(async)
        if (texture == null || !texture.isCreated()) { // char by char
            return drawTextCharByChar(
                x, y, key.createFont(), key.text, color,
                backgroundColor, key.widthLimit, key.heightLimit, alignX, alignY, false
            )
        }

        return drawText(x, y, color, backgroundColor, texture, alignX, alignY)
    }

    fun getTextSizeX(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        val size = getTextSize(font, text, widthLimit, heightLimit).value
            ?: return text.length * font.sampleWidth
        return getSizeX(size)
    }

    fun getTextSizeX(font: Font, text: CharSequence): Int {
        return getTextSizeX(font, text, -1, -1)
    }

    fun getTextSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): AsyncCacheData<Int> =
        FontManager.getSize(font, text, widthLimit, heightLimit)

    fun getTextSizeOr(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        return getTextSize(font, text, widthLimit, heightLimit).value
            ?: return getSize(font.sampleWidth * text.length, font.sizeInt)
    }

    fun getTextSize(key: TextCacheKey): AsyncCacheData<Int> = FontManager.getSize(key)
}