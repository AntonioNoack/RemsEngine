package me.anno.ui.base

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.hoveredPanel
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Maths.length
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

object Tooltips {

    var oldX = 0f
    var oldY = 0f

    var lastMovementTime = 0L

    val style  = DefaultConfig.style.getChild("tooltip")

    val textPanel = TextPanel("", style).apply {
        breaksIntoMultiline = true
    }

    val container = PanelContainer(textPanel, Padding(style.getSize("padding", 4)), style).apply {
        this += WrapAlign.LeftTop
    }

    private val tooltipReactionTime get() = DefaultConfig["ui.tooltip.reactionTime", 300]

    fun draw(): Boolean {

        if (tooltipReactionTime < 0) return false

        val dx = oldX - mouseX
        val dy = oldY - mouseY

        oldX = mouseX
        oldY = mouseY

        val time = GFX.gameTime

        if (length(dx, dy) > deltaTime) {// 1px / s
            lastMovementTime = time
            return false
        }

        val delta = abs(time - lastMovementTime) / 1_000_000

        if (delta >= tooltipReactionTime) {

            val w = GFX.width
            val h = GFX.height

            fun draw(panel: Panel) {
                val availableW = min(w, (textPanel.font.size * 20).toInt())
                panel.calculateSize(availableW, h)
                // container.applyConstraints()
                val x = min(mouseX.roundToInt() + 10, w - panel.minW)
                val y = min(mouseY.roundToInt() - 20, h - panel.minH)
                panel.placeInParent(x, y)
                panel.applyPlacement(panel.minW, panel.minH)
                panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
            }

            val panel = hoveredPanel?.getTooltipPanel(mouseX, mouseY)
            if (panel != null) {

                draw(panel)
                return true

            } else {

                val tooltipText = hoveredPanel?.getTooltipText(mouseX, mouseY)
                if (tooltipText != null && tooltipText.isNotBlank()) {
                    textPanel.text = tooltipText
                    draw(container)
                    return true
                }

            }

        }

        return false

    }

}