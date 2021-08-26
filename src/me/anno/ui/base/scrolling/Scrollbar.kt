package me.anno.ui.base.scrolling

import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mix
import kotlin.math.min

open class Scrollbar(style: Style) : Panel(style.getChild("scrollbar")) {

    // todo change color depending on hover x mouse down

    val activeAlpha = 0.2f

    val scrollColor = -1
    val scrollColorAlpha = 0.3f
    val scrollBackground = -1

    var wasActive = 0f

    fun multiplyAlpha(color: Int, mAlpha: Float): Int {
        val alpha = mAlpha * color.shr(24).and(255)
        return color.and(0xffffff) or clamp(alpha.toInt(), 0, 255).shl(24)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val isActive = Input.mouseX.toInt() in x0..x1 && Input.mouseY.toInt() in y0..y1
        wasActive = mix(wasActive, if (isActive) 1f else 0f, min(1f, 5f * deltaTime))

        drawRect(x0, y0, x1 - x0, y1 - y0, multiplyAlpha(scrollBackground, activeAlpha * wasActive))

    }

}