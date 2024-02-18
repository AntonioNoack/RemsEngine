package me.anno.fonts

import me.anno.fonts.keys.FontKey
import me.anno.utils.InternalAPI

object FontStats {

    @InternalAPI
    var getTextGeneratorImpl: ((FontKey) -> TextGenerator)? = null
    fun getTextGenerator(key: FontKey): TextGenerator {
        val tmp = getTextGeneratorImpl ?: throw NotImplementedError("Text generator")
        return tmp(key)
    }

    @InternalAPI
    var queryInstalledFontsImpl: (() -> Collection<String>)? = null
    fun queryInstalledFonts(): Collection<String> {
        val tmp = queryInstalledFontsImpl ?: throw NotImplementedError("Font list")
        return tmp()
    }

    @InternalAPI
    var getTextLengthImpl: ((Font, String) -> Double)? = null
    fun getTextLength(font: Font, text: String): Double {
        val tmp = getTextLengthImpl ?: throw NotImplementedError("Text length")
        return tmp(font, text)
    }

    @InternalAPI
    var getFontHeightImpl: ((Font) -> Double)? = null
    fun getFontHeight(font: Font): Double {
        val tmp = getFontHeightImpl ?: throw NotImplementedError("Text length")
        return tmp(font)
    }
}