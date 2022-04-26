package me.anno.ui.base.scrolling

import me.anno.Engine
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mulAlpha
import me.anno.ui.Panel
import me.anno.ui.style.Style
import kotlin.math.abs
import kotlin.math.min

open class Scrollbar(style: Style) : Panel(style.getChild("scrollbar")) {

    // todo change color when the mouse is being pressed

    val activeAlpha = 0.2f

    var minSize = 5

    val scrollColor = -1
    val scrollColorAlpha = 0.3f
    val scrollBackground = -1

    @NotSerializedProperty
    var alpha = 0f

    @NotSerializedProperty
    var isBeingHovered = false

    @NotSerializedProperty
    var lastTime = 0L

    fun updateAlpha(): Boolean {
        val oldAlpha = alpha
        val time = Engine.gameTime
        val dt = abs(time - lastTime) * 1e-9f
        lastTime = time
        alpha = mix(oldAlpha, if (isBeingHovered) 1f else 0f, min(1f, 5f * dt))
        return abs(alpha - oldAlpha) > 0.001f
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawRect(x0, y0, x1 - x0, y1 - y0, mulAlpha(scrollBackground, activeAlpha * alpha))
    }

}