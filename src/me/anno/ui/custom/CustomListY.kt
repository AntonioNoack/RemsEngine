package me.anno.ui.custom

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.types.Lists.sumByFloat
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CustomListY(style: Style): PanelListY(style), CustomList {

    init {
        spacing = style.getSize("custom.drag.size", 4)
        weight = 1f
    }

    override fun invalidateLayout() {
        window?.needsLayout?.add(this)
    }

    override fun getLayoutState(): Any = children.map { it.weight }

    override val customChildren
        get() = children.filter { it !is CustomizingBar }

    val minSize get() = 10f / h

    fun change(p: Panel, delta: Float){
        p.weight = clamp(p.weight + delta, minSize, 1f)
    }

    override fun remove(index: Int) {
        CustomListX.remove(this, index)
    }

    fun update(){
        children.forEachIndexed { index, panel ->
            (panel as? CustomizingBar)?.index = index
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        var hadVisibleChild = false
        children@ for(child in children){
            if(child.visibility == Visibility.VISIBLE){
                try {
                    hadVisibleChild = drawChild(child, x0, y0, x1, y1) or hadVisibleChild
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    override fun move(index: Int, delta: Float) {
        val c1 = children[index-1]
        val c2 = children[index+1]
        change(c1, +delta/h)
        change(c2, -delta/h)
        // (children.filter { it !is CustomizingBar }.joinToString { weights[it].toString() })
    }

    override fun add(child: Panel): PanelList {
        val index = children.size
        if(index > 0){
            super.add(CustomizingBar(index, 0, spacing, style))
        }
        child.weight = 1f
        return super.add(child)
    }

    fun add(child: Panel, weight: Float): PanelList {
        add(child)
        child.weight = weight
        return this
    }

    override fun addChild(panel: Panel) {
        add(panel, panel.weight)
    }

    override fun calculateSize(w: Int, h: Int) {
        this.w = w
        this.h = h
        minW = 10
        minH = 10
    }

    override fun placeInParent(x: Int, y: Int) {

        this.x = x
        this.y = y

        if(children.isEmpty()) return
        if(children.size == 1){

            val child = children.first()
            child.placeInParent(x, y)
            child.applyPlacement(w, h)
            // child.w = w
            // child.h = h
            // child.applyConstraints()

        } else {

            val minWeight = 0.0001f
            val available = h - (children.size/2) * spacing
            val sumWeight = children.filter { it !is CustomizingBar }.sumByFloat { max(it.weight, minWeight) }
            val weightScale = 1f / sumWeight

            var childY = this.y
            for(child in children){
                val weight = max(minWeight, child.weight)
                val childH = if(child is CustomizingBar)
                    child.minH
                else {
                    val betterWeight = max(weight * weightScale, minSize)
                    if(betterWeight != weight) child.weight = betterWeight
                    (betterWeight / sumWeight * available).roundToInt()
                }
                child.calculateSize(w, childH)
                child.place(x, childY, w, childH)
                childY += min(childH, child.h)
            }

        }

    }



}