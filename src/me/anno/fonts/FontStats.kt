package me.anno.fonts

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
    var getTextGeneratorImpl: ((FontKey) -> FontImpl<*>)? = null
    fun getTextGenerator(key: FontKey): FontImpl<*> {
        val tmp = getTextGeneratorImpl
            ?: return getFallbackFontGenerator(key)
        return tmp(key)
    }

    private fun getFallbackFontGenerator(key: FontKey): FontImpl<*> {
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
    var getTextLengthImpl1: ((Font, Int) -> Double)? = null
    fun getTextLength(font: Font, codepoint: Int): Double {
        val tmp = getTextLengthImpl1
            ?: return font.size * 0.6
        return tmp(font, codepoint)
    }

    @InternalAPI
    var getTextLengthImpl2: ((Font, Int, Int) -> Double)? = null
    fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Double {
        val tmp = getTextLengthImpl2
            ?: return font.size * 0.6
        return tmp(font, codepointA, codepointB)
    }

    val subpixelOffsetR = Vector2f()
    val subpixelOffsetB = Vector2f()
}