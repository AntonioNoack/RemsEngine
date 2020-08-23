package me.anno.objects.meshes

import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.cache.Cache
import me.anno.objects.meshes.obj.OBJReader
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File

class Mesh(var file: File, parent: Transform?): GFXTransform(parent){

    // todo types of lights
    // todo shadows, ...
    // todo types of shading/rendering?

    // for the start it is nice to be able to import meshes like a torus into the engine :)

    constructor(): this(File(""), null)

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val file = file

        // load the 3D model
        val data = Cache.getEntry(file, false, "Mesh", 1000, false){
            val attributes = listOf(
                Attribute("coords", 3),
                Attribute("uvs", 2),
                Attribute("normals", 3)
            )
            // load the model...
            // assume it's obj first...
            val obj = OBJReader(file)
            // generate mesh data from this obj somehow...
            val meshData = MeshData()
            meshData.toDraw = obj.pointsByMaterial.mapValues {
                val buffer = StaticFloatBuffer(attributes, it.value.size)
                it.value.forEach { v -> buffer.put(v) }
                buffer
            }
            meshData
        } as? MeshData
        if(isFinalRendering && data == null) throw MissingFrameException(file)

        data?.draw(stack, time, color) ?: super.onDraw(stack, time, color)

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("File", "", null, file, style){ file = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun getClassName() = "Mesh"
    override fun getDefaultDisplayName() = "Mesh"

}