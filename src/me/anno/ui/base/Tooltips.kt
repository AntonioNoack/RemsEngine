package me.anno.ui.base

import me.anno.Engine
import me.anno.Engine.deltaTime
import me.anno.config.DefaultConfig
import me.anno.gpu.WindowX
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.length
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextPanel
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.abs
import kotlin.math.min

object Tooltips {

    var oldX = 0f
    var oldY = 0f

    var lastMovementTime = 0L

    val style = DefaultConfig.style.getChild("tooltip")

    val textPanel = TextPanel("", style).apply {
        breaksIntoMultiline = true
    }

    val container = PanelContainer(textPanel, Padding(style.getSize("padding", 4)), style).apply {
        this += WrapAlign.LeftTop
    }

    private val tooltipReactionTime get() = DefaultConfig["ui.tooltip.reactionTime", 300]

    var lastPanel: Panel? = null

    fun draw(window: WindowX, sourcePanel: Panel, panel: Panel) {
        val w = window.width
        val h = window.height
        val fontSize = textPanel.font.sizeInt
        val availableW = min(w, fontSize * 20)
        panel.calculateSize(availableW, h)
        val window1 = panel.window
        val mouseX = window1?.mouseX ?: window.mouseX
        val mouseY = window1?.mouseY ?: window.mouseY
        // container.applyConstraints()
        val x = min(mouseX.toInt() + fontSize, w - panel.minW)
        val y = if (sourcePanel.y < fontSize) {
            // if panel is at the top, draw ttt below it, not above,
            // because it would cover the panel itself
            min(mouseY.toInt() + fontSize / 2, h - panel.minH)
        } else {
            min(mouseY.toInt() - fontSize, h - panel.minH)
        }
        panel.setPosition(x, y)
        panel.setSize(panel.minW, panel.minH)
        panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
    }

    fun draw(window: WindowX): Boolean {

        if (tooltipReactionTime < 0) return false

        val mouseX = window.mouseX
        val mouseY = window.mouseY

        val dx = oldX - mouseX
        val dy = oldY - mouseY

        oldX = mouseX
        oldY = mouseY

        val time = Engine.gameTime

        if (length(dx, dy) > deltaTime) {// 1px / s
            lastMovementTime = time
            return false
        }

        val delta = abs(time - lastMovementTime)
        val tooltipReactionTimeNanos = tooltipReactionTime * MILLIS_TO_NANOS
        if (delta >= tooltipReactionTimeNanos || lastPanel?.onMovementHideTooltip == false) {
            val hovered = StudioBase.instance?.hoveredPanel
            if (hovered != null) {

                val tooltip = hovered.getTooltipToP(mouseX, mouseY)
                lastPanel = tooltip as? Panel

                if (tooltip is Panel) {
                    tooltip.window = hovered.window
                    draw(window, hovered, tooltip)
                    return true
                } else if (tooltip is String && !tooltip.isBlank2()) {
                    textPanel.text = tooltip
                    draw(window, hovered, container)
                    return true
                }
            }
        } else lastPanel = null
        return false
    }

}