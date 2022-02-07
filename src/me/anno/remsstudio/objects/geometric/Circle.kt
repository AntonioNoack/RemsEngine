package me.anno.remsstudio.objects.geometric

import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.gpu.drawing.GFXx3Dv2
import me.anno.remsstudio.objects.GFXTransform
import me.anno.remsstudio.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4fc

open class Circle(parent: Transform? = null) : GFXTransform(parent) {

    var innerRadius = AnimatedProperty.float01()
    var startDegrees = AnimatedProperty(Type.ANGLE, 0f)
    var endDegrees = AnimatedProperty(Type.ANGLE, 360f)

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        GFXx3Dv2.draw3DCircle(this, time, stack, innerRadius[time], startDegrees[time], endDegrees[time], color)
    }

    override fun transformLocally(pos: Vector3fc, time: Double): Vector3f {
        return Vector3f(pos.x(), -pos.y(), pos.z()) // why ever y needs to be mirrored...
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val geo = getGroup("Geometry", "", "geometry")
        geo += vi("Inner Radius", "Relative size of hole in the middle", innerRadius, style)
        geo += vi("Start Degrees", "To cut a piece out of the circle", startDegrees, style)
        geo += vi("End Degrees", "To cut a piece out of the circle", endDegrees, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "innerRadius", innerRadius)
        writer.writeObject(this, "startDegrees", startDegrees)
        writer.writeObject(this, "endDegrees", endDegrees)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "innerRadius" -> innerRadius.copyFrom(value)
            "startDegrees" -> startDegrees.copyFrom(value)
            "endDegrees" -> endDegrees.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override val className get() = "Circle"
    override val defaultDisplayName get() = Dict["Circle", "obj.circle"]
    override val symbol get() = DefaultConfig["ui.style.circle", "◯"]

}