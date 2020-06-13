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
import me.anno.utils.length
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

object Tooltips {

    var oldX = 0f
    var oldY = 0f

    var lastMovementTime = 0L

    val style = DefaultConfig.style.getChild("tooltip")
    val textPanel = TextPanel("", style)
    val container = PanelContainer(
        textPanel, Padding(style.getSize("padding", 4)),
        style
    )

    val tooltipReactionTime = DefaultConfig["tooltip.reactionTime", 300]

    init {
        container += WrapAlign.LeftTop
    }

    fun draw(){

        if(tooltipReactionTime < 0) return

        val dx = oldX - mouseX
        val dy = oldY - mouseY

        oldX = mouseX
        oldY = mouseY

        val time = GFX.lastTime

        if(length(dx, dy) > deltaTime){// 1px / s
            lastMovementTime = time
            return
        }

        val delta = abs(time - lastMovementTime) / 1_000_000

        if(delta >= tooltipReactionTime){

            val tooltipText = hoveredPanel?.getTooltipText(mouseX, mouseY)
            if(tooltipText != null){
                textPanel.text = tooltipText
                container.calculateSize(GFX.width, GFX.height)
                container.applyConstraints()
                val x = min(mouseX.roundToInt() + 10, GFX.width - container.w)
                val y = min(mouseY.roundToInt() - 20 , GFX.height - container.h)
                container.placeInParent(x, y)
                GFX.clip(0, 0, GFX.width, GFX.height)
                container.draw(container.x, container.y, container.x + container.w, container.y + container.h)
            }

        }

    }

}