package me.anno.ui.custom

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.CustomizingBar
import me.anno.ui.style.Style
import me.anno.utils.clamp
import kotlin.math.max
import kotlin.math.roundToInt

class CustomListY(style: Style): PanelListY(style), CustomList {

    init {
        spacing = style.getSize("custom.drag.size", 2)
    }

    val minSize = 10f

    val weights = HashMap<Panel, Float>()

    fun change(p: Panel, delta: Float){
        weights[p] = clamp((weights[p] ?: 0f) + delta, minSize, h.toFloat())
    }

    override fun move(index: Int, delta: Float) {
        val c1 = children[index-1]
        val c2 = children[index+1]
        change(c1, +delta)
        change(c2, -delta)
        // println(children.filter { it !is CustomizingBar }.joinToString { weights[it].toString() })
    }

    override fun add(child: Panel): PanelList {
        val index = children.size
        if(index > 0){
            super.add(CustomizingBar(index, 0, spacing, style))
        }
        weights[child] = 100f
        return super.add(child)
    }

    fun add(child: Panel, weight: Float): PanelList {
        add(child)
        weights[child] = weight
        return this
    }

    override fun placeInParent(x: Int, y: Int) {

        this.x = x
        this.y = y

        if(children.isEmpty()) return
        if(children.size == 1){

            val child = children.first()
            child.placeInParent(x, y)
            child.w = w
            child.h = h
            child.applyConstraints()

        } else {

            val available = h - (children.size/2) * spacing
            val sumWeight = weights.values.sum()
            val weightScale = h / sumWeight

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
                child.applyConstraints()
                child.placeInParent(x, childY)
                childY += childH
            }

        }

    }



}