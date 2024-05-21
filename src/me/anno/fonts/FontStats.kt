package me.anno.fonts

import me.anno.fonts.keys.FontKey
import me.anno.utils.InternalAPI

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
        val tmp = getTextGeneratorImpl ?: return FallbackFontGenerator(key)
        return tmp(key)
    }

    @InternalAPI
    var queryInstalledFontsImpl: (() -> Collection<String>)? = null
    fun queryInstalledFonts(): Collection<String> {
        val tmp = queryInstalledFontsImpl ?: return emptyList()
        return tmp()
    }

    @InternalAPI
    var getTextLengthImpl: ((Font, String) -> Double)? = null
    fun getTextLength(font: Font, text: String): Double {
        val tmp = getTextLengthImpl ?: return text.length * font.size * 0.6
        return tmp(font, text)
    }

    @InternalAPI
    var getFontHeightImpl: ((Font) -> Double)? = null
    fun getFontHeight(font: Font): Double {
        val tmp = getFontHeightImpl ?: return font.size.toDouble()
        return tmp(font)
    }
}