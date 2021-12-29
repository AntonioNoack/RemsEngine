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
import me.anno.utils.maths.Maths.length
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

    fun draw(sourcePanel: Panel, panel: Panel) {
        val w = GFX.width
        val h = GFX.height
        val fontSize = textPanel.font.sizeInt
        val availableW = min(w, fontSize * 20)
        panel.calculateSize(availableW, h)
        // container.applyConstraints()
        val x = min(mouseX.toInt() + fontSize, w - panel.minW)
        val y = if (sourcePanel.y < fontSize) {
            // if panel is at the top, draw ttt below it, not above,
            // because it would cover the panel itself
            min(mouseY.toInt() + fontSize / 2, h - panel.minH)
        } else {
            min(mouseY.toInt() - fontSize, h - panel.minH)
        }
        panel.placeInParent(x, y)
        panel.applyPlacement(panel.minW, panel.minH)
        panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
    }

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
        if (delta >= tooltipReactionTime || lastPanel?.onMovementHideTooltip == false) {
            val hovered = hoveredPanel
            if (hovered != null) {

                val panel = hovered.getTooltipPanel(mouseX, mouseY)
                lastPanel = panel

                if (panel != null) {
                    draw(hovered, panel)
                    return true
                } else {
                    val tooltipText = hovered.getTooltipText(mouseX, mouseY)
                    if (tooltipText != null && !tooltipText.isBlank2()) {
                        textPanel.text = tooltipText
                        draw(hovered, container)
                        return true
                    }
                }
            }
        } else lastPanel = null
        return false
    }

}