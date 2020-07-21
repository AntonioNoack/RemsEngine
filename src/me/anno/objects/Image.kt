package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.whiteTexture
import me.anno.io.base.BaseWriter
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.objects.cache.SFBufferData
import me.anno.objects.meshes.svg.SVGMesh
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File

class Image(var file: File = File(""), parent: Transform? = null): GFXTransform(parent){

    var nearestFiltering = DefaultConfig["default.image.nearest", true]
    var tiling = AnimatedProperty.tiling()

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        val name = file.name
        when {
            name.endsWith("svg", true) -> {
                val bufferData = Cache.getEntry(file.absolutePath, "svg", 0, imageTimeout, true){
                    val svg = SVGMesh()
                    svg.parse(XMLReader.parse(file.inputStream().buffered()) as XMLElement)
                    SFBufferData(svg.buffer!!)
                } as? SFBufferData
                if(bufferData == null && isFinalRendering) throw MissingFrameException(file)
                if(bufferData != null){
                    // todo apply tiling...
                    GFX.draw3DSVG(stack, bufferData.buffer, whiteTexture, color, isBillboard[time], true)
                }
            }
            name.endsWith("webp", true) -> {
                val tiling = tiling[time]
                // calculate required scale? no, without animation, we don't need to scale it down ;)
                val texture = Cache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout)
                if((texture == null || !texture.isLoaded) && isFinalRendering) throw MissingFrameException(file)
                if(texture?.isLoaded == true) GFX.draw3D(stack, texture, color, isBillboard[time], nearestFiltering, tiling)
            }
            else -> {
                val tiling = tiling[time]
                val texture = Cache.getImage(file, imageTimeout, true)
                if(texture == null && isFinalRendering) throw MissingFrameException(file)
                texture?.apply {
                    GFX.draw3D(stack, texture, color, isBillboard[time], nearestFiltering, tiling)
                }
            }
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("File Location", "Texture file location", null, file, style){ file = it }
        list += VI("Tiling", "(tile count x, tile count y, offset x, offset y)", tiling, style)
        list += VI("Nearest Filtering", "Pixelated look; linear interpolation otherwise", null, nearestFiltering, style){ nearestFiltering = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("file", file.toString())
        writer.writeBool("nearestFiltering", nearestFiltering, true)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "nearestFiltering" -> nearestFiltering = value
            else -> super.readBool(name, value)
        }
    }

    override fun getClassName(): String = "Image"

    companion object {
        val imageTimeout = 5000L
    }

}