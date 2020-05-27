package me.anno.fonts

import me.anno.gpu.texture.Texture2D
import me.anno.ui.base.DefaultRenderingHints
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.lang.StrictMath.round
import java.text.AttributedString
import kotlin.streams.toList


class AWTFont(val font: Font): XFont {

    fun prepareGraphics(g2d: Graphics2D){
        g2d.font = font
        g2d.setRenderingHints(DefaultRenderingHints.hints as Map<*,*>)
    }

    val unused = BufferedImage(1,1,1).graphics as Graphics2D
    init {
        prepareGraphics(unused)
    }

    val fontMetrics = unused.fontMetrics

    fun containsSpecialChar(text: String): Boolean {
        for(cp in text.codePoints()){
            if(cp > 127) return true
        }
        return false
    }

    override fun generateTexture(text: String, fontSize: Float): Texture2D? {

        if(text.isBlank()) return null
        if(containsSpecialChar(text)) return generateTexture2(text, fontSize)

        val width = fontMetrics.stringWidth(text)
        val height = fontMetrics.height

        if(width < 1 || height < 1) return null

        val image = BufferedImage(width, height, 1)
        val gfx = image.graphics as Graphics2D
        prepareGraphics(gfx)

        val x = (image.width - width) * 0.5f
        val y = (image.height - height) * 0.5f + fontMetrics.ascent

        gfx.drawString(text, x, y)


        gfx.dispose()

        // val dataBuffer = GLFWImage.Buffer(GFX.loadImage("icon.png"))
        val texture = Texture2D(image.width, image.height)
        texture.create(image)

        // println("uploaded texture of size $width x $height for $text in ${font.name}:${font.size}")

        return texture
        // ImageIO.write(image, "png", File("C:/Users/Antonio/Desktop/text${text.hashCode()}.png"))

    }

    fun generateTexture2(text: String, fontSize: Float): Texture2D? {

        val withIcons = createFallbackString(text, font, getFallback(fontSize))
        val layout = TextLayout(withIcons.iterator, unused.fontRenderContext)
        val bounds = layout.bounds

        val width = ceil(bounds.width)
        val height = ceil(layout.ascent + layout.descent)

        // println("$height for ${layout.ascent} + ${layout.descent}")

        val image = BufferedImage(width, height, 1)
        val gfx = image.graphics as Graphics2D
        prepareGraphics(gfx)

        val x = (image.width - width) * 0.5f
        val y = (image.height - height) * 0.5f + layout.ascent

        gfx.drawString(withIcons.iterator, x, y)

        gfx.dispose()

        // val dataBuffer = GLFWImage.Buffer(GFX.loadImage("icon.png"))
        val texture = Texture2D(image.width, image.height)
        texture.create(image)

        // println("uploaded texture of size $width x $height for $text in ${font.name}:${font.size}")

        return texture

    }

    fun ceil(f: Float) = round(f + 0.5f)
    fun ceil(f: Double) = round(f + 0.5).toInt()

    private fun createFallbackString(
        text: String,
        mainFont: Font,
        fallbackFont: Font
    ): AttributedString {
        val result = AttributedString(text)
        val textLength = text.length
        result.addAttribute(TextAttribute.FONT, mainFont, 0, textLength)
        var fallback = false
        var fallbackBegin = 0
        val codePoints = text.codePoints().toList()
        for (i in codePoints.indices) {
            // 😉
            val inQuestion = codePoints[i]
            val curFallback = !mainFont.canDisplay(inQuestion)
            if(curFallback){
                println("${String(Character.toChars(inQuestion))}, $inQuestion needs fallback, supported? ${fallbackFont.canDisplay(inQuestion)}")
            }
            if (curFallback != fallback) {
                fallback = curFallback
                if (fallback) {
                    fallbackBegin = i
                } else {
                    result.addAttribute(TextAttribute.FONT, fallbackFont, fallbackBegin, i)
                }
            }
        }
        return result
    }

    companion object {
        val staticGfx = BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D
        val staticMetrics = staticGfx.fontMetrics
        val staticFontRenderCTX = staticGfx.fontRenderContext

        var fallbackFont0 = Font("Segoe UI Emoji", Font.PLAIN, 25)
        val fallbackFonts = HashMap<Float, Font>()
        fun getFallback(size: Float): Font {
            val cached = fallbackFonts[size]
            if(cached != null) return cached
            val font = fallbackFont0.deriveFont(size)
            fallbackFonts[size] = font
            return font
        }
    }
}