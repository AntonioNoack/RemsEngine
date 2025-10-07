package me.anno.jvm.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.CharacterOffsetCache
import me.anno.fonts.Codepoints
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.FontImpl
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphLayout
import me.anno.fonts.IEmojiCache
import me.anno.fonts.TextGenerator.Companion.TEXTURE_PADDING_H
import me.anno.fonts.TextGenerator.Companion.TEXTURE_PADDING_W
import me.anno.fonts.keys.FontKey
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawTexts.simpleChars
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.Color.undoPremultiply
import me.anno.utils.async.Callback
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Strings.isBlank
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.min

class AWTFont(
    override val fontKey: FontKey,
    val awtFont: Font
) : FontImpl<AWTFont>() {

    val engineFont = fontKey.toFont()

    private val fontMetrics = run {
        val unused = BufferedImage(1, 1, BI_FORMAT).graphics as Graphics2D
        unused.prepareGraphics(awtFont, false)
        unused.fontMetrics
    }

    private val renderContext by lazy {
        FontRenderContext(null, true, true)
    }

    // used in Rem's Studio -> cannot be private
    val exampleLayout by lazy {
        TextLayout("o", awtFont, renderContext)
    }

    override fun getSelfFont(): AWTFont = this
    override fun getFallbackFonts(size: Float) = getFallback(size)
    override fun getExampleAdvance(): Float = exampleLayout.advance
    override fun getAdvance(text: CharSequence, font: AWTFont): Float {
        var sum = 0f
        val cache = CharacterOffsetCache(engineFont)
        val codepoints = text.codepoints()
        for (i in codepoints.indices) {
            val curr = codepoints[i]
            val next = if (i + 1 >= codepoints.size) ' '.code else codepoints[i + 1]
            sum += cache.getOffset(curr, next)
            println("sum $sum += '${curr.joinChars()}'")
        }
        return sum
    }

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, codepoint: Int, dx: Float, dy: Float) {
        if (codepoint.isBlank()) return
        if (Codepoints.isEmoji(codepoint)) {
            // will be fixed later,
            // because we need the alpha-channel and subpixels are only supported on images without alpha
        } else if (codepoint < 127) {
            gfx.drawString(asciiStrings[codepoint], dx, dy)
        } else {
            gfx.drawString(codepoint.joinChars(), dx, dy)
        }
    }

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawEmoji(gfx: IntImage, codepoint: Int, dx: Float, dy: Float) {
        val fontSize = engineFont.sizeInt
        val emojiId = Codepoints.getEmojiId(codepoint)
        val emojiImage = IEmojiCache.emojiCache.getEmojiImage(emojiId, fontSize)
            .waitFor() ?: return
        val xi = (dx + fontSize * 0.1f).toIntOr()
        val yi = (dy - fontSize * 0.8f).toIntOr()
        emojiImage.forEachPixel { pxi, pyi ->
            val color = emojiImage.getRGB(pxi, pyi).undoPremultiply()
            gfx.setRGB(xi + pxi, yi + pyi, color)
        }
    }

    override fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        if (text.isEmpty()) return engineFont.emptySize.value!!
        val fontSize = engineFont.size
        val parts = GlyphLayout(
            engineFont, text,
            widthLimitToRelative(widthLimit, fontSize),
            heightLimitToMaxNumLines(heightLimit, fontSize)
        )
        val width = min(ceil(parts.width).toIntOr() + TEXTURE_PADDING_W, widthLimit)
        val height = min(ceil(parts.height).toIntOr() + TEXTURE_PADDING_H, heightLimit)
        return GFXx2D.getSize(width, height)
    }

    private fun Texture2D.createFromImage(image: Image, callback: Callback<ITexture2D>) {
        width = image.width
        height = image.height
        wasCreated = false
        image.createTexture(this, checkRedundancy = false, callback)
    }

    override fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int,
        backgroundColor: Int
    ) {

        val widthLimit = GFX.maxTextureSize
        val heightLimit = GFX.maxTextureSize

        val alignment = CharacterOffsetCache.getOffsetCache(engineFont)
        val size = alignment.getOffset('w'.code, 'w'.code)
        val width = min(widthLimit, ceil(size).toIntOr() + 1)
        val height = min(heightLimit, fontMetrics.height)

        val texture = Texture2DArray("awtAtlas", width, height, simpleChars.size)
        if (GFX.isGFXThread()) {
            createASCIITexture(texture, portableImages, textColor, backgroundColor)
            callback.ok(texture)
        } else {
            addGPUTask("awtAtlas", width, height, false) {
                createASCIITexture(texture, portableImages, textColor, backgroundColor)
                callback.ok(texture)
            }
        }
    }

    override fun getBaselineY(): Float {
        return exampleLayout.ascent
    }

    override fun getLineHeight(): Float {
        return exampleLayout.ascent + exampleLayout.descent
    }

    override fun getSupportLevel(fonts: List<AWTFont>, codepoint: Int, lastSupportLevel: Int): Int {
        if (Codepoints.isEmoji(codepoint) || (codepoint < 0xffff && codepoint.toChar() in " \t\r\n")) {
            return -1
        }

        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.awtFont.canDisplay(codepoint)) {
                return index
            }
        }

        LOGGER.warn("Glyph '$codepoint' cannot be displayed")
        return lastSupportLevel
    }

    override fun generateTexture(
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int,
        backgroundColor: Int
    ) {
        if (text.isEmpty())
            return callback.ok(TextureLib.blackTexture)

        val fontSize = engineFont.size
        val layout = GlyphLayout(
            engineFont, text,
            widthLimitToRelative(widthLimit, fontSize),
            heightLimitToMaxNumLines(heightLimit, fontSize)
        )

        val width = min(ceil(layout.width).toIntOr() + TEXTURE_PADDING_W, widthLimit)
        val height = min(ceil(layout.height).toIntOr() + TEXTURE_PADDING_H, heightLimit)

        if (layout.isEmpty() || width < 1 || height < 1) {
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-font-v3", width, height, 1)
        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (hasPriority) {
            createImage(texture, portableImages, textColor, backgroundColor, layout)
            callback.ok(texture)
        } else {
            addGPUTask("awt-font-v6", width, height, false) {
                createImage(texture, portableImages, textColor, backgroundColor, layout)
                callback.ok(texture)
            }
        }
    }

    private fun createImage(
        texture: Texture2D, portableImages: Boolean, textColor: Int, backgroundColor: Int,
        layout: GlyphLayout
    ) {

        val image = BufferedImage(texture.width, texture.height, BI_FORMAT)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)

        gfx.background = Color(backgroundColor)
        if (backgroundColor.and(0xffffff) != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, image.width, image.height)
        }
        gfx.color = Color(textColor)

        val fonts = getFontAndFallbacks(engineFont.size)
        val y = exampleLayout.ascent
        var lastFontIndex = -1
        for (glyphIndex in layout.indices) {
            val codepoint = layout.getCodepoint(glyphIndex)
            if (!Codepoints.isEmoji(codepoint)) {
                val fontIndex = layout.getFontIndex(glyphIndex)
                if (fontIndex != lastFontIndex) {
                    // s.font != this when the character is unsupported, e.g., for emojis
                    gfx.font = fonts[fontIndex].awtFont
                    lastFontIndex = fontIndex
                }
                val dx = layout.getX0(glyphIndex)
                val dy = layout.getY(glyphIndex) + y
                drawString(gfx, codepoint, dx, dy)
            } // else will be drawn later
        }
        gfx.dispose()

        val imageI = image.toImage(true) as IntImage
        imageI.fillAlpha(0)
        for (glyphIndex in layout.indices) {
            val codepoint = layout.getCodepoint(glyphIndex)
            if (Codepoints.isEmoji(codepoint)) {
                val dx = layout.getX0(glyphIndex)
                val dy = layout.getY(glyphIndex) + y
                drawEmoji(imageI, codepoint, dx, dy)
            }
        }
        texture.createFromImage(imageI, Callback.printError())
    }

    private fun createASCIITexture(
        texture: Texture2DArray,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int
    ) {
        val image = BufferedImage(texture.width, texture.height * texture.layers, BI_FORMAT)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)
        if (backgroundColor != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, image.width, image.height)
        }
        gfx.color = Color(textColor)
        var y = fontMetrics.ascent.toFloat()
        val dy = texture.height.toFloat()
        for (yi in simpleChars.indices) {
            // not necessary on desktop, but improves quality on Android, because mono somehow is not mono :)
            val width = TextLayout(simpleChars[yi], gfx.font, renderContext).bounds.maxX.toFloat()
            gfx.drawString(simpleChars[yi], (texture.width - width) * 0.5f, y)
            y += dy
        }
        gfx.dispose()

        val imageI = image.toImage(true)
        (imageI as IntImage).fillAlpha(0)

        // there are no emojis in this range -> they can be skipped

        texture.create(imageI, sync = true)
    }

    override fun toString(): String = engineFont.toString()

    companion object {

        private val LOGGER = LogManager.getLogger(AWTFont::class)

        /**
         * Must not have alpha channel for subpixel-data!
         * We render our text-textures the following way:
         *  - text has alpha = 0 (alpha is unsupported anyway)
         *  - emojis have alpha >= 0
         * */
        private const val BI_FORMAT = BufferedImage.TYPE_INT_RGB

        private val asciiStrings = List(128) { it.toChar().toString() }

        private val fallbackFontList = DefaultConfig[
            "ui.font.fallbacks",
            "Segoe UI Emoji,Segoe UI Symbol,DejaVu Sans,FreeMono,Unifont,Symbola"
        ].split(',').mapNotNull { if (it.isBlank2()) null else it.trim() }

        private val fallbackFonts = HashMap<Float, List<AWTFont>>()
        fun getFallback(size: Float): List<AWTFont> {
            val cached = fallbackFonts[size]
            if (cached != null) return cached
            val fonts = fallbackFontList.mapNotNull {
                FontManager.getFont(
                    it, size, bold = false, italic = false,
                    4f, 0f
                ) as? AWTFont
            }
            fallbackFonts[size] = fonts
            return fonts
        }
    }
}