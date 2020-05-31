package me.anno.objects

import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.FontMesh
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f

class Text(var text: String, parent: Transform?): GFXTransform(parent){

    var font = "Verdana"
    var buffer: FontMesh? = null

    var isBold = false
    var isItalic = false

    var lastText = text
    var lastFont = font
    var lastBold = isBold
    var lastItalic = isItalic

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){

        if(text.isNotBlank()){
            if(buffer == null || lastText != text || lastFont != font || lastBold != isBold || lastItalic != isItalic){
                buffer?.buffer?.destroy()
                // todo get italic + boldness correctly from font manager
                val awtFont = FontManager.getFont(font, 20f, isBold, isItalic)
                buffer = FontMesh((awtFont as AWTFont).font, text)
                lastText = text
                lastFont = font
                lastItalic = isItalic
                lastBold = isBold
            }

            val buffer = buffer!!

            GFX.draw3D(stack, buffer.buffer, GFX.whiteTexture, getLocalColor(), isBillboard[time])

        }

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "text" -> text = value
            else -> super.readString(name, value)
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += TextInput("Text", style, text)
            .setChangeListener { text = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
        val fontList = ArrayList<String>()
        FontManager.requestFontList { it ->
            synchronized(fontList){
                fontList += it
            }
        }
        list += EnumInput("Font", "", fontList, style)
            .setChangeListener { font = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Italic", isItalic, style)
            .setChangeListener { isItalic = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Bold", isBold, style)
            .setChangeListener { isBold = it }
            .setIsSelectedListener { show(null) }
    }

    override fun getClassName(): String = "Text"

}