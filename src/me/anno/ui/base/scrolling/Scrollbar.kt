package me.anno.ui.base.scrolling

import me.anno.Time.uiDeltaTime
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.mulAlpha
import kotlin.math.abs

open class Scrollbar(style: Style) : Panel(style.getChild("scrollbar")) {

    constructor(parent: Panel, style: Style) : this(style) {
        this.parent = parent
    }

    val activeAlpha = 0.2f

    var minSize = 5

    val scrollColor = -1
    val scrollColorAlpha = 0.3f
    val scrollBackground = -1

    @NotSerializedProperty
    var alpha = 0f

    fun updateAlpha(parent: Panel) {
        val oldAlpha = alpha
        alpha = mix(
            oldAlpha, if (isHovered) if (Input.isLeftDown) 1f else 0.8f else 0f,
            dtTo01(10f * uiDeltaTime.toFloat())
        )
        if (abs(alpha - oldAlpha) > 0.001f) {
            parent.invalidateDrawing(lx0, ly0, lx1, ly1)
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawRect(x0, y0, x1 - x0, y1 - y0, scrollBackground.mulAlpha(activeAlpha * alpha))
    }

    override fun clone(): Scrollbar {
        val clone = Scrollbar(style)
        copyInto(clone)
        return clone
    }
}