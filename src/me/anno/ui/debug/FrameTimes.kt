package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.ui.base.Panel
import kotlin.math.max

object FrameTimes : Panel(DefaultConfig.style) {

    val width = 200
    val height = 50

    override fun calculateSize(w: Int, h: Int) {
        minW = width
        minH = height
    }

    var maxValue = 0f
    val values = FloatArray(width)
    var nextIndex = 0
    fun putValue(value: Float) {
        values[nextIndex] = value
        nextIndex = (nextIndex + 1) % width
        val max = values.max()!!
        maxValue = max(maxValue * 0.995f, max)
    }

    fun draw() {
        canBeSeen = true
        draw(x, y, x + w, y + h)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground()
        // GFX.drawRect(x0, y0, x1-x0, y1-y0, black)
        val indexOffset = nextIndex - 1 + width
        for (x in x0 until x1) {
            val i = x - this.x
            val v = values[(indexOffset + i) % width]
            val barHeight = (height * v / maxValue).toInt()
            val barColor = -1
            GFX.drawRect(x, y + height - barHeight, 1, barHeight, barColor)
        }
    }

}