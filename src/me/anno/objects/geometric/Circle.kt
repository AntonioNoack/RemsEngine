package me.anno.objects.geometric

import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min

class Circle(parent: Transform?): GFXTransform(parent){

    var innerRadius = AnimatedProperty.float()
    var startDegrees = AnimatedProperty.float()
    var endDegrees = AnimatedProperty.float().set(360f)

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {
        GFX.draw3DCircle(stack, innerRadius[time], startDegrees[time], endDegrees[time], color, isBillboard[time])
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput(style, "Inner Radius", innerRadius[lastLocalTime], AnimatedProperty.Type.FLOAT)
            .setChangeListener { putValue(innerRadius, it) }
            .setIsSelectedListener { show(innerRadius) }
        list += FloatInput(style, "Start Degrees", startDegrees[lastLocalTime], AnimatedProperty.Type.FLOAT)
            .setChangeListener { putValue(startDegrees, it) }
            .setIsSelectedListener { show(startDegrees) }
        list += FloatInput(style, "End Degrees", endDegrees[lastLocalTime], AnimatedProperty.Type.FLOAT)
            .setChangeListener { putValue(endDegrees, it) }
            .setIsSelectedListener { show(endDegrees) }
    }

    override fun getClassName(): String = "Circle"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "innerRadius", innerRadius)
        writer.writeObject(this, "startDegrees", startDegrees)
        writer.writeObject(this, "endDegrees", endDegrees)
    }

}