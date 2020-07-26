package me.anno.ui.base.scrolling

import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.utils.clamp
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListMultiline
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min

open class ScrollPanelY(child: Panel, padding: Padding,
                        style: Style,
                        alignX: AxisAlignment): PanelContainer(child, padding, style){

    constructor(style: Style, padding: Padding, align: AxisAlignment): this(PanelListY(style), padding, style, align)

    init {
        child += WrapAlign(alignX, AxisAlignment.MIN)
        weight = 0.0001f
    }

    var scrollPosition = 0f
    val maxLength = 100_000
    var isDownOnScrollbar = false

    val maxScrollPosition get() = max(0, child.minH + padding.height - h)
    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbar.width", 8)
    val scrollbarPadding = style.getSize("scrollbar.padding", 1)
    init {
        padding.right += scrollbarWidth
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        // if(h > GFX.height) throw RuntimeException()

        child.calculateSize(w-padding.width, maxLength)
        child.applyConstraints()

        minW = child.minW + padding.width
        minH = child.minH + padding.height

        /*if(child is PanelListMultiline){
            println("${child.minW} ${child.minH} -> $minW $minH inside $w $h, makes $maxScrollPosition")
        }*/

        // if(h > GFX.height) throw RuntimeException()

        // warn("scroll fini $x += $w ($minW), $y += $h ($minH) by ${child.h}, ${child.minH}")

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scroll = scrollPosition.toInt()
        child.placeInParent(x+padding.left,y+padding.top-scroll)

    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.draw(x0, y0, x1, y1)
        if(maxScrollPosition > 0f){
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.w = scrollbarWidth
            scrollbar.h = h - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if(!Input.isShiftDown){
            val delta = dx-dy
            val scale = 20f
            if((delta > 0f && scrollPosition >= maxScrollPosition) ||
                (delta < 0f && scrollPosition <= 0f)){// if done scrolling go up the hierarchy one
                super.onMouseWheel(x, y, dx, dy)
            } else {
                scrollPosition += scale * delta
                clampScrollPosition()
            }
        } else super.onMouseWheel(x, y, dx, dy)
    }

    fun clampScrollPosition(){
        scrollPosition = clamp(scrollPosition, 0f, maxScrollPosition.toFloat())
    }

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        isDownOnScrollbar = scrollbar.contains(x,y,scrollbarPadding*2)
        if(!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(isDownOnScrollbar){
            scrollbar.onMouseMoved(x, y, dx, dy)
            clampScrollPosition()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun getClassName(): String = "ScrollPanelY"

}