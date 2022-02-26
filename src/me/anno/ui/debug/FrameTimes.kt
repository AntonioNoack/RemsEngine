package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.maths.Maths.mixARGB
import me.anno.utils.OS
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

    fun putTime(value: Float) {
        putValue(value, textColor)
    }

    fun putValue(value: Float, color: Int) {
        val containers = containers
        for (i in containers.indices) {
            val container = containers[i]
            if (container.color == color) {
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

    val drawInts get() = OS.isAndroid

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)

        val containers = containers
        if (containers.isEmpty()) return

        containers.sortDescending()
        val maxValue = containers[0].maxValue

        for (j in containers.indices) {
            val container = containers[j]
            val nextIndex = container.nextIndex
            val values = container.values
            val barColor = container.color
            val indexOffset = nextIndex - 1 + width

            if (drawInts) {

                var lastX = x0
                var lastBarHeight = 0

                for (x in x0 until x1) {
                    val i = x - this.x
                    val v = values[(indexOffset + i) % width]
                    val barHeight = (height * v / maxValue).toInt()
                    if (barHeight != lastBarHeight) {
                        drawLine(lastX, x, lastBarHeight, barColor)
                        lastX = x
                        lastBarHeight = barHeight
                    }
                }

                drawLine(lastX, x1, lastBarHeight, barColor)

            } else {

                val yMax = y + height
                val bgc = backgroundColor
                for (x in x0 until x1) {
                    val i = x - this.x
                    val v = values[(indexOffset + i) % width]
                    val hf = height * v / maxValue
                    if (hf > 0f) {
                        val hi = hf.toInt()
                        drawLine(x, x + 1, hi, barColor)
                        drawRect(x, yMax - hi, 1, hi, barColor)
                        drawRect(x, yMax - hi - 1, 1, 1, mixARGB(bgc, barColor, hf - hi))
                    }
                }

            }

        }

    }

    // to reduce draw calls by bundling stacks of the same height
    fun drawLine(lastX: Int, nextX: Int, barHeight: Int, barColor: Int) {
        if (lastX < nextX) {
            drawRect(lastX, y + height - barHeight, nextX - lastX, barHeight, barColor)
        }
    }

}