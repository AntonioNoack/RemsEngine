package me.anno.fonts

import me.anno.fonts.Codepoints.countCodepoints
import me.anno.fonts.keys.FontKey
import me.anno.image.ImageCache
import me.anno.utils.InternalAPI
import org.joml.Vector2f

object FontStats {

    @InternalAPI
    var getDefaultFontSizeImpl: (() -> Int)? = null
    fun getDefaultFontSize(): Int {
        val tmp = getDefaultFontSizeImpl ?: return 15
        return tmp()
    }

    @InternalAPI
    var getTextGeneratorImpl: ((FontKey) -> TextGenerator)? = null
    fun getTextGenerator(key: FontKey): TextGenerator {
        val tmp = getTextGeneratorImpl
            ?: return getFallbackFontGenerator(key)
        return tmp(key)
    }

    private fun getFallbackFontGenerator(key: FontKey): TextGenerator {
        return if ("png" in ImageCache.streamReaders) AtlasFontGenerator(key)
        else LinesFontGenerator(key)
    }

    @InternalAPI
    var queryInstalledFontsImpl: (() -> Collection<String>)? = null
    fun queryInstalledFonts(): Collection<String> {
        val tmp = queryInstalledFontsImpl
            ?: return emptyList()
        return tmp()
    }

    @InternalAPI
    var getTextLengthImpl: ((Font, String) -> Double)? = null
    fun getTextLength(font: Font, text: String): Double {
        val tmp = getTextLengthImpl
            ?: return text.countCodepoints() * font.size * 0.6
        return tmp(font, text)
    }

    val subpixelOffsetR = Vector2f()
    val subpixelOffsetB = Vector2f()
}