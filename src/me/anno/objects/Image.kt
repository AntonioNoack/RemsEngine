package me.anno.objects

import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File

class Image(var file: File, parent: Transform?): GFXTransform(parent){

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {
        val texture = Cache.getImage(file)
        texture?.apply {
            GFX.draw3D(stack, texture, color, isBillboard[time])
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += TextInput("File Location", style, file.toString())
            .setChangeListener { file = File(it) }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("file", file.toString())
    }

    override fun readString(name: String, value: String) {
        when(name){
            "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun getClassName(): String = "Image"

}