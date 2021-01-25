package me.anno.ui.custom

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Lists.sumByFloat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class CustomListX(style: Style) : PanelListX(style), CustomList {

    init {
        spacing = style.getSize("custom.drag.size", 4)
        weight = 1f
    }

    override fun invalidateLayout() {
        window!!.needsLayout += this
    }

    override fun getLayoutState(): Any? {
        val weights = children.map { it.weight }
        return Pair(super.getLayoutState(), weights)
    }

    override val customChildren
        get() = children.filter { it !is CustomizingBar }

    companion object {
        fun remove(vg: PanelList, index: Int) {
            vg.apply {
                if (children.size > 1) {
                    if (index > 0) {
                        children.removeAt(index)
                        children.removeAt(index - 1)
                    } else {
                        children.removeAt(index + 1)
                        children.removeAt(index)
                    }
                    (vg as? CustomListX)?.update()
                    (vg as? CustomListY)?.update()
                } else {
                    // todo remove the last child -> remove this from our parent
                    (parent as? CustomList)?.remove(indexInParent)
                }
            }
        }
    }

    val minSize get() = 10f / w

    fun change(p: Panel, delta: Float) {
        p.weight = clamp(p.weight + delta, minSize, 1f)
    }

    fun update() {
        children.forEachIndexed { index, panel ->
            (panel as? CustomizingBar)?.index = index
        }
    }

    override fun remove(index: Int) {
        remove(this, index)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        var hadVisibleChild = false
        children@ for (child in children) {
            if (child.visibility == Visibility.VISIBLE) {
                try {
                    hadVisibleChild = drawChild(child, x0, y0, x1, y1) or hadVisibleChild
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun move(index: Int, delta: Float) {
        val c1 = children[index - 1]
        val c2 = children[index + 1]
        change(c1, +delta / w.toFloat())
        change(c2, -delta / w.toFloat())
        // (children.filter { it !is CustomizingBar }.joinToString { weights[it].toString() })
    }

    override fun add(child: Panel): PanelList {
        val index = children.size
        if (index > 0) {
            super.add(CustomizingBar(index, spacing, 0, style))
        }
        if(child.weight <= 0f) child.weight = 1f
        child.parent = this
        return super.add(child)
    }

    override operator fun plusAssign(child: Panel){
        add(child)
    }

    fun add(child: Panel, weight: Float): PanelList {
        add(child)
        child.weight = weight
        return this
    }

    override fun addChild(panel: Panel) {
        add(panel)
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

        if (children.isEmpty()) return
        if (children.size == 1) {

            val child = children.first()
            child.placeInParent(x, y)
            child.applyPlacement(w, h)

        } else {

            val minWeight = 0.0001f
            val available = w - (children.size / 2) * spacing
            val sumWeight = children.filter { it !is CustomizingBar }.sumByFloat { max(minWeight, it.weight) }
            val weightScale = 1f / sumWeight

            var childX = this.x
            for (child in children) {
                val weight = max(minWeight, child.weight)
                val childW = if (child is CustomizingBar)
                    child.minW
                else {
                    val betterWeight = max(weight * weightScale, minSize)
                    if (betterWeight != weight) child.weight = betterWeight
                    (betterWeight / sumWeight * available).roundToInt()
                }
                child.calculateSize(childW, h)
                child.place(childX, y, childW, h)
                childX += min(childW, child.w)
            }

        }

    }


}