package me.anno.objects.geometric

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f

class Circle(parent: Transform? = null): GFXTransform(parent){

    var innerRadius = AnimatedProperty.float01()
    var startDegrees = AnimatedProperty.float()
    var endDegrees = AnimatedProperty.float().set(360f)

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {
        GFX.draw3DCircle(stack, innerRadius[time], startDegrees[time], endDegrees[time], color, isBillboard[time])
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Inner Radius", "Relative size of hole in the middle", innerRadius, style)
        list += VI("Start Degrees", "To cut a piece out of the circle", startDegrees, style)
        list += VI("End Degrees", "To cut a piece out of the circle", endDegrees, style)
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

}