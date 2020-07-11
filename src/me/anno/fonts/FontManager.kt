package me.anno.fonts

import me.anno.gpu.GFX
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.objects.cache.Cache
import me.anno.objects.cache.TextureCache
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

object FontManager {

    val LOGGER = LogManager.getLogger(FontManager::class)!!

    private val awtFontList = ArrayList<String>()
    private val awtFonts = HashMap<String, Font>()

    private val fonts = HashMap<String, XFont>()

    fun requestFontList(callback: (List<String>) -> Unit){
        if(awtFontList.isNotEmpty()) callback(awtFontList)
        else {
            thread {
                val t0 = System.nanoTime()
                val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                val fontNames = ge.availableFontFamilyNames
                synchronized(awtFontList){
                    awtFontList.clear()
                    awtFontList += fontNames
                }
                val t1 = System.nanoTime()
                // 0.17s on Win 10, R5 2600, a few extra fonts
                // this lag would not be acceptable :)
                // worst-case-scenario: list too long, and no fonts are returned
                // (because of that, the already used one is added)
                LOGGER.info("Used ${(t1-t0)*1e-9f} to get font list")
                callback(awtFontList)
            }
        }
    }

    private fun getFontSizeIndex(fontSize: Float): Int = round(100.0 * ln(fontSize)).toInt()
    private fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun getString(fontName: String, fontSize: Float, text: String, bold: Boolean, italic: Boolean): ITexture2D? {
        if(text.isEmpty()) return null
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 4 + (if(bold) 1 else 0) + (if(italic) 2 else 0)
        val cache = Cache.getEntry(fontName, text, sub, fontTimeout, !GFX.loadTexturesSync){
            // println("Created texture for $text")
            val font = getFont(fontName, fontSize, fontSizeIndex, italic, bold)
            val averageFontSize = getAvgFontSize(fontSizeIndex)
            val texture = font.generateTexture(text, averageFontSize)
            TextureCache(texture)
        } as? TextureCache
        return cache?.texture
    }

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
        val font2 = AWTFont(awtFonts[awtName] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, fontSize) ?: throw RuntimeException("Font $name was not found"))
        fonts[awtName] = font2
        return font2
    }

    private fun getDefaultFont(name: String): Font? {
        val cached = awtFonts[name]
        if(cached != null) return cached
        val font = Font.decode(name) ?: return null
        awtFonts[name] = font
        return font
    }

    val fontTimeout = 1000L

}