package me.anno.fonts

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
    var getTextGeneratorImpl: (() -> FontImpl<*>)? = null
    fun getFontImpl(): FontImpl<*> {
        val tmp = getTextGeneratorImpl
            ?: return getFallbackFontGenerator()
        return tmp()
    }

    private fun getFallbackFontGenerator(): FontImpl<*> {
        return if ("png" in ImageCache.streamReaders) AtlasFontGenerator
        else LinesFontGenerator
    }

    @InternalAPI
    var queryInstalledFontsImpl: (() -> Collection<String>)? = null
    fun queryInstalledFonts(): Collection<String> {
        val tmp = queryInstalledFontsImpl
            ?: return emptyList()
        return tmp()
    }

    val subpixelOffsetR = Vector2f()
    val subpixelOffsetB = Vector2f()
}