package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.warn
import kotlin.math.max


class PanelListMultiline(sorter: Comparator<Panel>?, style: Style): PanelList(sorter, style){

    var scaleChildren = true
    var childWidth: Int
    var childHeight: Int

    init {
        val defaultSize = style.getSize("textSize", 12) * 10
        childWidth = style.getSize("childWidth", defaultSize)
        childHeight = style.getSize("childHeight", defaultSize)
    }

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        columns = max(1, (w+spacing)/(childWidth+spacing))
        rows = max(1, (children.size + columns - 1) / columns)

        val childScale = if(scaleChildren) max(1f, ((w+spacing)/columns - spacing)*1f/childWidth) else 1f

        calcChildWidth = if(scaleChildren) (childWidth * childScale).toInt() else childWidth
        calcChildHeight = if(scaleChildren) (childHeight * childScale).toInt() else childHeight

        for(child in children){
            child.calculateSize(calcChildWidth, calcChildHeight)
            child.applyConstraints()
        }

        minW = (calcChildWidth + spacing) * columns - spacing
        minH = (calcChildHeight + spacing) * rows - spacing

        this.h = minH

        // warn("multiline ${children.size} in $w ($minW) x $h ($minH) -> $rows x $columns")
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        for((i, child) in children.withIndex()){
            val ix = i % columns
            val iy = i / columns
            val cx = x + ix * (calcChildWidth + spacing) - spacing
            val cy = y + iy * (calcChildHeight + spacing) - spacing
            child.placeInParent(cx, cy)
        }

    }

}