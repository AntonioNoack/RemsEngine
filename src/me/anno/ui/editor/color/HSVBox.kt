package me.anno.ui.editor.color

import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import org.joml.Vector3fc

open class HSVBox(
    val chooser: ColorChooser,
    val v0: Vector3fc,
    val du: Vector3fc,
    val dv: Vector3fc,
    val dh: Float,
    style: Style,
    size: Float,
    val onValueChanged: (Float, Float) -> Unit
) : Panel(style) {

    init {
        minH = (size * style.getSize("textSize", 14)).toInt()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        chooser.drawColorBox(this, v0, du, dv, dh, false)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "selectColor" -> {
                if (dragged == null) {
                    onValueChanged((x - this.x) / w, 1f - (y - this.y) / h)
                }
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun getClassName() = "HSVBox"

}