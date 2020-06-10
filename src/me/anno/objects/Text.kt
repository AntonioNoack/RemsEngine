package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.FontMesh
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import kotlin.collections.ArrayList
import kotlin.math.max

class Text(var text: String, parent: Transform?): GFXTransform(parent){

    // todo text shadow
    // todo multiple lines
    // how we apply sampling probably depends on our AA solution...

    var font = "Verdana"

    var isBold = false
    var isItalic = false

    var fmKey = FontMeshKey(font, isBold, isItalic, text)

    data class FontMeshKey(
        val fontName: String, val isBold: Boolean, val isItalic: Boolean,
        val text: String){
        fun equals(fontName: String, isBold: Boolean, isItalic: Boolean, text: String) =
            fontName == this.fontName && isBold == this.isBold && isItalic == this.isItalic && text == this.text
    }

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){

        if(text.isNotBlank()){

            if(!fmKey.equals(font, isBold, isItalic, text)){
                fmKey = FontMeshKey(font, isBold, isItalic, text)
            }

            val fontMesh = Cache.getEntry(fmKey){
                val awtFont = FontManager.getFont(font, 20f, isBold, isItalic)
                val buffer = FontMesh((awtFont as AWTFont).font, text)
                buffer
            } as FontMesh

            GFX.draw3D(stack, fontMesh.buffer, GFX.whiteTexture, color, isBillboard[time], true)

            // todo calculate (signed) distance fields for different kinds of shadows from the mesh
            // bad solution for blurred shadows
            /*color.w *= 0.001f
            val random = Random()
            for(i in 0 until 1000){
                stack.pushMatrix()
                val radius = 0.01f
                stack.translate(radius * random.nextGaussian().toFloat(), radius * random.nextGaussian().toFloat(), 0f)
                GFX.draw3D(stack, buffer.buffer, GFX.whiteTexture, color, isBillboard[time], true)
                stack.popMatrix()
            }*/

        }

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
        writer.writeString("font", font)
        writer.writeBool("isItalic", isItalic, true)
        writer.writeBool("isBold", isBold, true)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "text" -> text = value
            "font" -> font = value
            else -> super.readString(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "isBold" -> isBold = value
            "isItalic" -> isItalic = value
            else -> super.readBool(name, value)
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += TextInput("Text", style, text)
            .setChangeListener { text = it }
            .setIsSelectedListener { GFX.selectedProperty = null }

        val fontList = ArrayList<String>()
        fontList += font
        fontList += GFX.menuSeparator

        fun sortFavourites(){
            fontList.sort()
            val lastUsedSet = lastUsedFonts.toHashSet()
            fontList.sortByDescending { if(it == GFX.menuSeparator) 1 else if(it in lastUsedSet) 2 else 0 }
        }

        FontManager.requestFontList { systemFonts ->
            synchronized(fontList){
                fontList += systemFonts.filter { it != font }
                sortFavourites()
            }
        }

        list += EnumInput("Font", true, font, fontList, style)
            .setChangeListener {
                putLastUsedFont(it)
                sortFavourites()
                font = it }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Italic", isItalic, style)
            .setChangeListener { isItalic = it }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Bold", isBold, style)
            .setChangeListener { isBold = it }
            .setIsSelectedListener { show(null) }

    }

    override fun getClassName(): String = "Text"

    companion object {
        // todo save the last used fonts? yes :)
        // todo per project? idk
        val lastUsedFonts = arrayOfNulls<String>(max(0, DefaultConfig["lastUsed.fonts.count", 5]))
        fun putLastUsedFont(font: String){
            if(lastUsedFonts.isNotEmpty()){
                for(i in lastUsedFonts.indices){
                    if(lastUsedFonts[i] == font) return
                }
                for(i in 0 until lastUsedFonts.lastIndex){
                    lastUsedFonts[i] = lastUsedFonts[i+1]
                }
                lastUsedFonts[lastUsedFonts.lastIndex] = font
            }
        }
    }

}