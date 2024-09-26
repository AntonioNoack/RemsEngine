package me.anno.ui.base

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.gpu.OSWindow
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextPanel
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isNotBlank2
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

    val container = PanelContainer(textPanel, Padding(style.getSize("padding", 4)), style)

    private val tooltipReactionTime get() = DefaultConfig["ui.tooltip.reactionTime", 300]

    var lastPanel: Panel? = null

    fun draw(window: OSWindow, sourcePanelY: Int, panel: Panel) {
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
        val y = if (sourcePanelY < fontSize) {
            // if panel is at the top, draw ttt below it, not above,
            // because it would cover the panel itself
            min(mouseY.toInt() + fontSize / 2, h - panel.minH)
        } else {
            min(mouseY.toInt() - fontSize, h - panel.minH)
        }
        panel.setPosSize(x, y, panel.minW, panel.minH)
        panel.draw(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height)
    }

    fun draw(window: OSWindow): Boolean {

        if (tooltipReactionTime < 0) return false

        val mouseX = window.mouseX
        val mouseY = window.mouseY

        val dx = oldX - mouseX
        val dy = oldY - mouseY

        oldX = mouseX
        oldY = mouseY

        val time = Time.nanoTime

        if (length(dx, dy) > Time.uiDeltaTime) {// 1px / s
            lastMovementTime = time
            return false
        }

        val delta = abs(time - lastMovementTime)
        val tooltipReactionTimeNanos = tooltipReactionTime * MILLIS_TO_NANOS
        if (delta >= tooltipReactionTimeNanos || lastPanel?.onMovementHideTooltip == false) {
            if (window.progressBars.isNotEmpty()) {
                val progressbarHeight = window.progressbarHeight
                val pbi = mouseY.toInt() / max(progressbarHeight, 1)
                val pb = window.progressBars.getOrNull(pbi)
                if (pb != null && pb.name.isNotBlank2()) {
                    textPanel.text = pb.name
                    draw(window, pbi * progressbarHeight, container)
                    return true
                }
            }
            val hovered = EngineBase.instance?.hoveredPanel
            if (hovered != null) {

                val tooltip = hovered.getTooltipToP(mouseX, mouseY)
                lastPanel = tooltip as? Panel

                if (tooltip is Panel) {
                    tooltip.window = hovered.window
                    draw(window, hovered.y, tooltip)
                    return true
                } else if (tooltip is String && !tooltip.isBlank2()) {
                    textPanel.text = tooltip
                    draw(window, hovered.y, container)
                    return true
                }
            }
        } else lastPanel = null
        return false
    }
}