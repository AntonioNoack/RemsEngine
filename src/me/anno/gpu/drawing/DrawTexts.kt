package me.anno.gpu.drawing

import me.anno.fonts.Font
import me.anno.fonts.FontImpl.Companion.heightLimitToMaxNumLines
import me.anno.fonts.FontImpl.Companion.widthLimitToRelative
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphLayout
import me.anno.fonts.keys.CharCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
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
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.types.Strings.splitLines
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import kotlin.math.max
import kotlin.math.min

object DrawTexts {

    private val LOGGER = LogManager.getLogger(DrawTexts::class)

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

    fun drawText(
        x: Int, y: Int, padding: Int,
        font: Font, text: CharSequence,
        textColor: Int,
        backgroundColor: Int,
    ): Int = drawText(
        x, y, padding,
        font, text,
        textColor, backgroundColor,
        -1, -1
    )

    fun drawText(
        x: Int, y: Int, padding: Int,
        font: Font, text: CharSequence,
        textColor: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int {

        if ('\n' in text) {
            var sizeX = 0
            val split = text.splitLines()
            val lineOffset = font.lineHeightI
            for (index in split.indices) {
                val size = drawText(
                    x, y + index * lineOffset, font, split[index],
                    textColor, backgroundColor,
                    widthLimit, heightLimit, alignX, alignY,
                )
                sizeX = max(getSizeX(size), sizeX)
            }
            return getSize(sizeX, (split.size - 1) * lineOffset + font.lineHeightI)
        }

        val shader = chooseShader(textColor, backgroundColor)
        GFX.check()

        // todo optimized version without allocation, if widthLimit = -1 && heightLimit = -1
        val layout = GlyphLayout(
            font, text,
            widthLimitToRelative(widthLimit, font.size),
            heightLimitToMaxNumLines(heightLimit, font.size)
        )

        val x = x + getOffset(layout.width, alignX)
        val y = y + getOffset(layout.height, alignY)

        if (backgroundColor.a() != 0) {
            DrawRectangles.drawRect(
                x - padding, y - padding,
                layout.width + 2 * padding, layout.height + 2 * padding,
                backgroundColor
            )
        }

        GFX.loadTexturesSync.push(true)

        if (shader is ComputeShader) {
            drawTextLine(shader, x, y, font, layout, 0, 2)
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            if (layout.size > 1) {
                drawTextLine(shader, x, y, font, layout, 1, 2)
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            }
        } else {
            drawTextLine(shader, x, y, font, layout, 0, 1)
        }

        GFX.loadTexturesSync.pop()

        return getSize(layout.width, layout.height)
    }

    private fun drawTextLine(
        shader: GPUShader, x: Int, y: Int,
        font: Font, layout: GlyphLayout, i0: Int, step: Int
    ) {
        forLoop(i0, layout.size, step) { index ->
            val codepoint = layout.getCodepoint(index)
            val dx = layout.getX0(index)
            val dy = layout.getY(index)
            val key = CharCacheKey(font, codepoint, disableSubpixelRendering)
            val texture = FontManager.getTexture(key)
                .waitFor("drawTextCharByChar")
            if (texture != null && texture.wasCreated) {
                texture.bind(0, Filtering.TRULY_NEAREST, Clamping.CLAMP_TO_BORDER)
                drawChar(shader, texture, x + dx, y + dy, codepoint)
            }
        }
    }

    var disableSubpixelRendering = false
    fun chooseShader(textColor: Int, backgroundColor: Int, instanced: Int = 0): GPUShader {
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

    private fun drawChar(
        shader: GPUShader, texture: ITexture2D?,
        x2: Int, y2: Int, txt: Int
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
                }
            }
        } else {
            LOGGER.warn(
                "Texture for #$txt is ${
                    if (texture == null) "null"
                    else if (texture.isDestroyed) "destroyed"
                    else "not created"
                }, $texture"
            )
        }
    }

    fun drawText(
        x: Int, y: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int = drawText(
        x, y, 0, font, text,
        color, backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int = drawText(x, y, 0, text, alignX, alignY)

    fun drawText(
        x: Int, y: Int, padding: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int = drawText(
        x, y, padding, monospaceFont, text,
        FrameTimings.textColor, FrameTimings.backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int, padding: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int = drawText(
        x, y, padding, font, text,
        color, backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int,
        padding: Int, text: CharSequence,
    ): Int = drawText(
        x, y, padding, monospaceFont, text,
        FrameTimings.textColor,
        FrameTimings.backgroundColor,
    )

    fun drawText(
        x: Int, y: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN
    ): Int = drawText(
        x, y, 0, font, text,
        color, backgroundColor,
        widthLimit, heightLimit,
        alignX, alignY
    )

    fun getOffset(size: Int, alignment: AxisAlignment): Int {
        return alignment.getOffset(0, size - 1)
    }

    fun getTextSizeX(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        return getSizeX(getTextSize(font, text, widthLimit, heightLimit))
    }

    fun getTextSizeX(font: Font, text: CharSequence): Int {
        return getTextSizeX(font, text, -1, -1)
    }

    fun getTextSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int =
        FontManager.getSize(font, text, widthLimit, heightLimit)

}