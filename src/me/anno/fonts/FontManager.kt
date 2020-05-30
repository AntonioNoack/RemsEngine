package me.anno.fonts

import me.anno.gpu.texture.Texture2D
import me.anno.objects.cache.Cache
import me.anno.objects.cache.TextureCache
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.lang.RuntimeException
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

object FontManager {

    init {
        // todo this is a bottleneck with 0.245s
        // todo therefore this should be parallized with other stuff...
        /*val t0 = System.nanoTime()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontNames = ge.availableFontFamilyNames
        val t1 = System.nanoTime()
        for(fontName in fontNames){
            fontMap[fontName] = Font.decode(fontName)
        }
        val t2 = System.nanoTime()
        println("used ${(t1-t0)*1e-9f}+${(t2-t1)*1e-9f}s to get font list")*/
    }

    fun getFontSizeIndex(fontSize: Float): Int = round(100.0 * ln(fontSize)).toInt()
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun getString(fontName: String, fontSize: Float, text: String): Texture2D? {
        if(text.isBlank()) return null
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val cache = Cache.getEntry(fontName, text, fontSizeIndex){
            val font = getFont(fontName, fontSize, fontSizeIndex)
            val averageFontSize = getAvgFontSize(fontSizeIndex)
            val texture = font.generateTexture(text, averageFontSize)
            TextureCache(texture)
        } as TextureCache
        return cache.texture
    }

    val fonts = HashMap<String, XFont>()

    fun getFont(name: String, fontSize: Float): XFont {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val averageFontSize = getAvgFontSize(fontSizeIndex)
        return getFont(name, averageFontSize, fontSizeIndex)
    }

    private fun getFont(name: String, fontSize: Float, fontSizeIndex: Int): XFont {
        val font = fonts[name]
        if(font != null) return font
        val awtName = "$name:$fontSizeIndex"
        val font2 = AWTFont(fontMap[awtName] ?: Font.decode(name)?.deriveFont(fontSize) ?: throw RuntimeException("Font $name was not found"))
        fonts[awtName] = font2
        return font2
    }

    val fontMap = HashMap<String, Font>()

}