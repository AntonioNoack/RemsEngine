package me.anno.objects

import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.FontMesh
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f

class SimpleText(var text: String, parent: Transform?): GFXTransform(parent){

    var font = FontManager.getFont("Verdana", 20f)
    var buffer: FontMesh? = null
    var lastText = text


    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){

        if(text.isNotBlank()){
            if(buffer == null || lastText != text){
                buffer?.buffer?.destroy()
                buffer = FontMesh((font as AWTFont).font, text)
                lastText = text
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
    }

    override fun getClassName(): String = "SimpleText"

}