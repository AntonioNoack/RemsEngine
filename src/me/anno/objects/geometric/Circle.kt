package me.anno.objects.geometric

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXx3D.draw3DCircle
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class Circle(parent: Transform? = null): GFXTransform(parent){

    var innerRadius = AnimatedProperty.float01()
    var startDegrees = AnimatedProperty(Type.ANGLE, 0f)
    var endDegrees = AnimatedProperty(Type.ANGLE,360f)

    override fun getSymbol() = DefaultConfig["ui.style.circle", "◯"]

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        draw3DCircle(this, time, stack, innerRadius[time], startDegrees[time], endDegrees[time], color)
    }

    override fun transformLocally(pos: Vector3f, time: Double): Vector3f {
        return Vector3f(pos.x, -pos.y, pos.z) // why ever y needs to be mirrored...
    }

    override fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, id: String) -> SettingCategory) {
        super.createInspector(list, style, getGroup)
        val geo = getGroup("Geometry", "geometry")
        geo += VI("Inner Radius", "Relative size of hole in the middle", innerRadius, style)
        geo += VI("Start Degrees", "To cut a piece out of the circle", startDegrees, style)
        geo += VI("End Degrees", "To cut a piece out of the circle", endDegrees, style)
    }

    override fun getClassName(): String = "Circle"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "innerRadius", innerRadius)
        writer.writeObject(this, "startDegrees", startDegrees)
        writer.writeObject(this, "endDegrees", endDegrees)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "innerRadius" -> innerRadius.copyFrom(value)
            "startDegrees" -> startDegrees.copyFrom(value)
            "endDegrees" -> endDegrees.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    companion object {

        var isInited = false
        lateinit var buffer: StaticBuffer

        fun drawBuffer(shader: Shader){
            if(!isInited){
                createBuffer()
                isInited = true
            }
            buffer.draw(shader)
        }

        fun createBuffer(){
            val n = 36 * 4
            // angle, scaling
            buffer = StaticBuffer(listOf(Attribute("attr0", 2)), 3 * 2 * n)
            fun put(index: Int, scaling: Float){
                buffer.put(index.toFloat()/n, scaling)
            }
            for(i in 0 until n){
                val j = i+1
                put(i, 0f)
                put(i, 1f)
                put(j, 1f)
                put(i, 0f)
                put(j, 1f)
                put(j, 0f)
            }
        }

    }

}