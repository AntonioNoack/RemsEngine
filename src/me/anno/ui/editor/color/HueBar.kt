package me.anno.ui.editor.color

import me.anno.config.DefaultStyle
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.style.Style
import org.joml.Vector3f
import kotlin.math.roundToInt

class HueBar(chooser: ColorChooser, style: Style) : HSVBox(chooser,
    Vector3f(0f, 1f, 0.75f),
    Vector3f(1f, 0f, 0f),
    Vector3f(), 0f, style, 1f, { hue, _ ->
        chooser.setHSL(hue, chooser.saturation, chooser.lightness, chooser.opacity, chooser.colorSpace, true)
    }) {
    override fun getVisualState() = chooser.hue
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val x = x0 + ((x1 - x0) * chooser.hue).roundToInt()
        drawRect(x, y0, 1, y1 - y0, DefaultStyle.black)
    }

    override val className: String = "HueBar"
}