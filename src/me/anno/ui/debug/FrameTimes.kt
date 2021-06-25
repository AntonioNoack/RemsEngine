package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import kotlin.math.max

object FrameTimes : Panel(DefaultConfig.style.getChild("fps")) {

    val width = 200 * max(DefaultConfig.style.getSize("fontSize", 12), 12) / 12
    val height = width / 4

    val colors = TextPanel("", style)
    val textColor = colors.textColor

    override fun calculateSize(w: Int, h: Int) {
        minW = width
        minH = height
    }

    val timeContainer = TimeContainer(width, textColor)
    val containers = arrayListOf(timeContainer)

    fun putTime(value: Float){
        putValue(value, textColor)
    }

    fun putValue(value: Float, color: Int){
        for(container in containers){
            if(container.color == color){
                container.putValue(value)
                return
            }
        }
        val container = TimeContainer(width, color)
        containers.add(container)
        container.putValue(value)
    }

    fun draw() {
        canBeSeen = true
        draw(x, y, x + w, y + h)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground()

        if(containers.isEmpty()) return

        val maxValue = containers.maxOf { it.maxValue }

        containers.sortByDescending { it.maxValue }
        for(container in containers){
            val nextIndex = container.nextIndex
            val values = container.values
            val barColor = container.color
            val indexOffset = nextIndex - 1 + width
            for (x in x0 until x1) {
                val i = x - this.x
                val v = values[(indexOffset + i) % width]
                val barHeight = (height * v / maxValue).toInt()
                drawRect(x, y + height - barHeight, 1, barHeight, barColor)
            }
        }

    }

}