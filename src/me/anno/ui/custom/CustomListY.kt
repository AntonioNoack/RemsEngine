package me.anno.ui.custom

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.clamp
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CustomListY(style: Style): PanelListY(style), CustomList {

    init {
        spacing = style.getSize("custom.drag.size", 4)
    }

    val minSize get() = 10f / h

    val weights = HashMap<Panel, Float>()

    fun change(p: Panel, delta: Float){
        weights[p] = clamp((weights[p] ?: 0f) + delta, minSize, 1f)
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
        weights[child] = 1f
        return super.add(child)
    }

    fun add(child: Panel, weight: Float): PanelList {
        add(child)
        weights[child] = weight/100f
        return this
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

            val available = h - (children.size/2) * spacing
            val sumWeight = weights.values.sum()
            val weightScale = 1f / sumWeight

            var childY = this.y
            for(child in children.filter { it.visibility != Visibility.GONE }){
                val weight = weights[child]
                val childH = if(weight == null)
                    child.minH
                else {
                    val betterWeight = max(weight * weightScale, minSize)
                    if(betterWeight != weight) weights[child] = betterWeight
                    (betterWeight / sumWeight * available).roundToInt()
                }
                child.calculateSize(w, childH)
                // child.applyConstraints()
                child.placeInParent(x, childY)
                child.applyPlacement(w, childH)
                childY += min(childH, child.h)
            }

        }

    }



}