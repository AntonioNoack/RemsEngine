package me.anno.jvm.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.FontManager
import me.anno.fonts.LineSplitter
import me.anno.fonts.StringPart
import me.anno.fonts.TextGenerator
import me.anno.fonts.TextGenerator.Companion.TEXTURE_PADDING_H
import me.anno.fonts.TextGenerator.Companion.TEXTURE_PADDING_W
import me.anno.fonts.TextGroup
import me.anno.fonts.keys.FontKey
import me.anno.fonts.mesh.CharacterOffsetCache
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawTexts.simpleChars
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.jvm.images.BIImage.createBufferedImage
import me.anno.jvm.images.BIImage.createFromBufferedImage
import me.anno.jvm.images.BIImage.toImage
import me.anno.maths.Maths.clamp
import me.anno.utils.async.Callback
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.shorten
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class AWTFont(
    override val fontKey: FontKey,
    val awtFont: Font
) : LineSplitter<AWTFont>(), TextGenerator {

    val engineFont = fontKey.toFont()

    private val fontMetrics = run {
        val unused = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).graphics as Graphics2D
        unused.prepareGraphics(awtFont, false)
        unused.fontMetrics
    }

    private fun containsSpecialChar(self: CharSequence): Boolean {
        val limit = 127.toChar()
        for (charIndex in self.indices) {
            val codepoint = self[charIndex]
            if (codepoint == '\n' || codepoint == '\t' || codepoint > limit) return true
        }
        return false
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
    override fun getAdvance(text: CharSequence, font: AWTFont): Float =
        TextLayout(text.toString(), font.awtFont, renderContext).advance

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: CharSequence, group: TextGroup?, y: Int) =
        drawString(gfx, text, group, 0f, y.toFloat())

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: CharSequence, group: TextGroup?, x: Float, y: Float) {
        val group2 = group ?: createGroup(engineFont, text)
        // some distances still are awkward, because it is using the closest position, not float
        // (useful for "I"s)
        // maybe we could implement detecting, which sections need int positions, and which don't...
        if (containsSpecialChar(text)) {
            for ((index, char) in text.codepoints().withIndex()) {
                gfx.drawString(
                    char.joinChars().toString(),
                    x + group2.offsets[index].toFloat(), y
                )
            }
        } else {
            for (index in text.indices) {
                val char = text[index]
                gfx.drawString(
                    asciiStrings[char.code],
                    x + group2.offsets[index].toFloat(), y
                )
            }
        }
    }

    override fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        if (text.isEmpty()) return GFXx2D.getSize(0, fontMetrics.height)
        return if (containsSpecialChar(text) || (widthLimit in 0 until GFX.maxTextureSize)) {
            val parts = splitParts(
                text, engineFont.size, 4f, 0f,
                widthLimit.toFloat(), heightLimit.toFloat()
            )
            val width = min(ceil(parts.width).toInt(), widthLimit)
            val height = min(ceil(parts.height).toInt(), heightLimit)
            return GFXx2D.getSize(width, height)
        } else {
            val baseWidth = getStringWidth(createGroup(engineFont, text))
            val width = clamp(baseWidth.roundToIntOr() + 1, 0, GFX.maxTextureSize)
            val height = min(fontMetrics.height, GFX.maxTextureSize)
            GFXx2D.getSize(width, height)
        }
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

        if (text.isEmpty()) {
            return callback.ok(TextureLib.blackTexture)
        }

        if (containsSpecialChar(text) || widthLimit < text.length * engineFont.size * 2f) {
            return generateTextureV3(
                text, engineFont.size, widthLimit, heightLimit, portableImages,
                textColor, backgroundColor, callback
            )
        }

        val group = createGroup(engineFont, text)
        val width = min(widthLimit, ceil(getStringWidth(group)).toIntOr() + TEXTURE_PADDING_W)

        val lineCount = 1
        val fontHeight = fontMetrics.height
        val height = min(heightLimit, fontHeight * lineCount + TEXTURE_PADDING_H)

        if (width < 1 || height < 1) {
            return callback.ok(TextureLib.blackTexture)
        }
        if (max(width, height) > GFX.maxTextureSize) {
            return callback.err(
                IllegalArgumentException(
                    "Texture for text is too large! $width x $height > ${GFX.maxTextureSize}, " +
                            "${text.length} chars, $lineCount lines, ${awtFont.name} ${engineFont.size} px, ${
                                text.toString().shorten(200)
                            }"
                )
            )
        }

        if (text.isBlank2()) {
            // we need some kind of wrapper around texture2D
            // and return an empty/blank texture
            // that the correct size is returned is required by text input fields
            // (with whitespace at the start or end)
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-" + text.shorten(24), width, height, 1)
        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        val image = createImage(
            width,
            height, portableImages, textColor,
            backgroundColor, text, group
        )
        if (hasPriority) {
            createTexture(texture, image, callback)
        } else {
            addGPUTask("awt-font-v5", width, height, false) {
                createTexture(texture, image, callback)
            }
        }
    }

    private fun createTexture(texture: Texture2D, image: BufferedImage, callback: Callback<ITexture2D>) {
        texture.createFromBufferedImage(image, callback)
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

    private fun createImage(
        width: Int, height: Int, portableImages: Boolean,
        textColor: Int, backgroundColor: Int,
        text: CharSequence, group: TextGroup?,
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)

        if (backgroundColor != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, width, height)
        }

        gfx.color = Color(textColor)

        val y = fontMetrics.ascent
        // println("generating texture for '$text', size $fontSize with ascent $ascent")
        drawString(gfx, text, group, y)
        gfx.dispose()
        return image
    }

    override fun getSupportLevel(fonts: List<AWTFont>, char: Int, lastSupportLevel: Int): Int {
        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.awtFont.canDisplay(char)) {
                return index
            }
        }
        LOGGER.warn("Glyph '$char' cannot be displayed")
        return lastSupportLevel
    }

    private fun generateTextureV3(
        text: CharSequence,
        fontSize: Float,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int,
        callback: Callback<ITexture2D>
    ) {

        val parts = splitParts(text, fontSize, 4f, 0f, widthLimit.toFloat(), heightLimit.toFloat())
        val result = parts.parts

        val width = min(ceil(parts.width).toIntOr() + TEXTURE_PADDING_W, widthLimit)
        val height = min(ceil(parts.height).toIntOr() + TEXTURE_PADDING_H, heightLimit)

        if (result.isEmpty() || width < 1 || height < 1) {
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-font-v3", width, height, 1)
        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (hasPriority) {
            createImage(texture, portableImages, textColor, backgroundColor, result)
            callback.ok(texture)
        } else {
            addGPUTask("awt-font-v6", width, height, false) {
                createImage(texture, portableImages, textColor, backgroundColor, result)
                callback.ok(texture)
            }
        }
    }

    private fun createImage(
        texture: Texture2D, portableImages: Boolean, textColor: Int, backgroundColor: Int,
        result: List<StringPart>
    ) {
        val image = BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_RGB)
        // for (i in width-10 until width) image.setRGB(i, 0, 0xff0000)

        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)
        gfx.background = Color(backgroundColor)
        if (backgroundColor.and(0xffffff) != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, image.width, image.height)
        }
        gfx.color = Color(textColor)

        val y = exampleLayout.ascent

        for (part in result) {
            val bitmap = part.bitmap
            if (bitmap != null) {
                val tmpImage = bitmap.createBufferedImage(withAlpha = true)
                gfx.drawImage(tmpImage, part.xPos.toIntOr(), part.yPos.toIntOr(), null)
            } else {
                // s.font != this when the character is unsupported, e.g., for emojis
                gfx.font = (part.font as AWTFont).awtFont
                (part.font as AWTFont).drawString(gfx, part.text, null, part.xPos, part.yPos + y)
            }
        }

        gfx.dispose()
        texture.createFromBufferedImage(image, Callback.printError())
    }

    private fun createASCIITexture(
        texture: Texture2DArray,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int
    ) {
        val image = BufferedImage(texture.width, texture.height * texture.layers, BufferedImage.TYPE_INT_RGB)
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
        texture.create(image.toImage(), sync = true)
    }

    override fun toString(): String = engineFont.toString()

    companion object {

        private val LOGGER = LogManager.getLogger(AWTFont::class)

        private fun getStringWidth(group: TextGroup) = group.offsets.last() - group.offsets.first()
        private fun createGroup(font: me.anno.fonts.Font, text: CharSequence): TextGroup = TextGroup(font, text, 0.0)

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
                FontManager.getFont(it, size, bold = false, italic = false) as? AWTFont
            }
            fallbackFonts[size] = fonts
            return fonts
        }
    }
}