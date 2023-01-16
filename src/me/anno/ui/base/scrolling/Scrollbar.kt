package me.anno.ui.base.scrolling

import me.anno.Engine.deltaTime
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mulAlpha
import me.anno.ui.Panel
import me.anno.ui.style.Style
import kotlin.math.abs

open class Scrollbar(style: Style) : Panel(style.getChild("scrollbar")) {

    val activeAlpha = 0.2f

    var minSize = 5

    val scrollColor = -1
    val scrollColorAlpha = 0.3f
    val scrollBackground = -1

    @NotSerializedProperty
    var alpha = 0f

    @NotSerializedProperty
    var isBeingHovered = false

    fun updateAlpha(): Boolean {
        val oldAlpha = alpha
        alpha = mix(oldAlpha, if (isBeingHovered) if (Input.isLeftDown) 1f else 0.8f else 0f, dtTo01(10f * deltaTime))
        return abs(alpha - oldAlpha) > 0.001f
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawRect(x0, y0, x1 - x0, y1 - y0, mulAlpha(scrollBackground, activeAlpha * alpha))
    }

}