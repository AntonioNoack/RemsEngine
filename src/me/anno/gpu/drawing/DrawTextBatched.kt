package me.anno.gpu.drawing

import me.anno.fonts.Font
import me.anno.fonts.FontImpl.Companion.simpleChars
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphLayout
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawCurves.putRGBA
import me.anno.gpu.drawing.DrawTexts.chooseShader
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.types.Strings.splitLines
import kotlin.math.max

/**
 * Use this class, when you have a regular grid of chars, and need to draw lots of text (code editors).
 * */
object DrawTextBatched {

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
        x: Int, y: Int, padding: Int,
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

        val y2 = y + dy0 + padding - 1
        val x2 = x + dx0 + padding + (charWidth - texture.width) / 2

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

        return width
    }

    fun getTextSizeCharByChar(font: Font, text: CharSequence, equalSpaced: Boolean): Int {

        if ('\n' in text) {
            var sizeX = 0
            val split = text.splitLines()
            val lineOffset = font.lineHeightI
            for (index in split.indices) {
                val size = getTextSizeCharByChar(font, split[index], equalSpaced)
                sizeX = max(getSizeX(size), sizeX)
            }
            return getSize(sizeX, (split.size - 1) * lineOffset + font.lineHeightI)
        }

        if (text.isEmpty())
            return font.emptySize

        return if (equalSpaced) {
            val charWidth = font.sampleWidth
            val textWidth = charWidth * text.length
            getSize(textWidth, font.lineHeightI)
        } else {
            val parts = GlyphLayout(font, text, 0f, Int.MAX_VALUE)
            getSize(parts.width, font.lineHeightI)
        }
    }

    fun getOffset(size: Int, alignment: AxisAlignment): Int {
        return alignment.getOffset(0, size - 1)
    }

}