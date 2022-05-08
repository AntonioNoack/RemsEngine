package me.anno.ui.base.scrolling

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.maths.Maths.mulAlpha
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style
import kotlin.math.max

open class ScrollbarX(val scrollable: ScrollableX, style: Style) : Scrollbar(style) {

    final override var parent: PrefabSaveable?
        get() = super.parent
        set(value) {
            super.parent = value
        }

    init {
        parent = scrollable as PanelGroup
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val relativePosition = scrollable.scrollPositionX / scrollable.maxScrollPositionX
        val barW = max(minSize.toDouble(), relativeSize * w)
        val barX = x + relativePosition * (w - barW)

        val color = mulAlpha(scrollColor, scrollColorAlpha + activeAlpha * alpha)
        drawRect(barX.toInt(), y0, barW.toInt(), y1 - y0, color)

    }

    val relativeSize
        get(): Double {
            val child = scrollable.child
            val minW = if (child is LongScrollable) child.sizeX else child.minW.toLong()
            return scrollable.w.toDouble() / minW
        }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            scrollable.scrollPositionX += dx / relativeSize
        }// else super.onMouseMoved(x, y, dx, dy)
    }

}