package me.anno.ui.editor.color

import me.anno.input.Input
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.ui.Panel
import me.anno.ui.Style
import org.joml.Vector3f

open class HSVBox(
    val chooser: ColorChooser,
    val v0: Vector3f,
    val du: Vector3f,
    val dv: Vector3f,
    val dh: Float,
    style: Style,
    size: Float,
    val onValueChanged: (Float, Float) -> Unit
) : Panel(style) {

    val minH1 = (size * style.getSize("fontSize", 14)).toInt()

    override fun calculateSize(w: Int, h: Int) {
        minH = minH1
        minW = w
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        chooser.drawColorBox(this, v0, du, dv, dh, false)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown && isInFocus && contains(x, y)) {
            if (dragged == null) {
                onValueChanged((x - this.x) / width, 1f - (y - this.y) / height)
            }
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override val className: String get() = "HSVBox"
}