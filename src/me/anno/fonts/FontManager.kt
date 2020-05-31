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

    // todo get real font list
    fun requestFontList(callback: (List<String>) -> Unit){
        callback(listOf("Verdana", "Serif", "Arial", "Times New Roman"))
    }

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

    fun getString(fontName: String, fontSize: Float, text: String, bold: Boolean, italic: Boolean): Texture2D? {
        if(text.isBlank()) return null
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 4 + (if(bold) 1 else 0) + (if(italic) 2 else 0)
        val cache = Cache.getEntry(fontName, text, sub){
            val font = getFont(fontName, fontSize, fontSizeIndex, italic, bold)
            val averageFontSize = getAvgFontSize(fontSizeIndex)
            val texture = font.generateTexture(text, averageFontSize)
            TextureCache(texture)
        } as? TextureCache
        return cache?.texture
    }

    val fonts = HashMap<String, XFont>()

    fun getFont(name: String, fontSize: Float, bold: Boolean, italic: Boolean): XFont {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val averageFontSize = getAvgFontSize(fontSizeIndex)
        return getFont(name, averageFontSize, fontSizeIndex, bold, italic)
    }

    private fun getFont(name: String, fontSize: Float, fontSizeIndex: Int, bold: Boolean, italic: Boolean): XFont {
        val font = fonts[name]
        if(font != null) return font
        val boldItalicStyle = (if(italic) Font.ITALIC else 0) or (if(bold) Font.BOLD else 0)
        val awtName = "$name:$fontSizeIndex:$boldItalicStyle"
        val font2 = AWTFont(fontMap[awtName] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, fontSize) ?: throw RuntimeException("Font $name was not found"))
        fonts[awtName] = font2
        return font2
    }

    private fun getDefaultFont(name: String): Font? {
        val cached = fontMap[name]
        if(cached != null) return cached
        val font = Font.decode(name) ?: return null
        fontMap[name] = font
        return font
    }

    val fontMap = HashMap<String, Font>()

}