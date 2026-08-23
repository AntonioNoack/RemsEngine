package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.Canvas
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector3f

class HueBar(chooser: ColorChooser, style: Style) : HSVBox(chooser,
    Vector3f(0f, 1f, 0.75f),
    Vector3f(1f, 0f, 0f),
    Vector3f(), 0f, style, 1f, { hue, _ ->
        chooser.setHSL(hue, chooser.saturation, chooser.lightness, chooser.opacity, chooser.colorSpace, 1, true)
    }) {
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val x = canvas.x0 + (canvas.dx * chooser.hue).roundToIntOr()
        drawRect(x, canvas.y0, 1, canvas.dy, black)
    }
}