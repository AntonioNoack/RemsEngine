package me.anno.ui.custom

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.bugs.SumOf
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class CustomList(val isY: Boolean, style: Style) : PanelListX(style) {

    init {
        spacing = style.getSize("custom.drag.size", 4)
        weight = 1f
    }

    override fun invalidateLayout() {
        window?.needsLayout?.add(this)
    }

    override fun getLayoutState(): Any? {
        return children.map { it.weight }
    }

    val customChildren
        get() = children.filter { it !is CustomizingBar }

    val minSize get() = if (isY) 10f / h else 10f / w

    fun change(p: Panel, delta: Float) {
        p.weight = clamp(p.weight + delta, minSize, 1f)
    }

    fun update() {
        children.forEachIndexed { index, panel ->
            (panel as? CustomizingBar)?.index = index
        }
    }

    fun remove(index: Int) {
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

    fun move(index: Int, delta: Float) {
        val c1 = children[index - 1]
        val c2 = children[index + 1]
        val deltaS = delta / (if (isY) h else w)
        change(c1, +deltaS)
        change(c2, -deltaS)
    }

    override fun add(child: Panel): PanelList {
        val index = children.size
        if (index > 0) {
            super.add(
                if (isY) CustomizingBar(index, 0, spacing, style)
                else CustomizingBar(index, spacing, 0, style)
            )
        }
        if (child.weight <= 0f) child.weight = 1f
        child.parent = this
        return super.add(child)
    }

    override operator fun plusAssign(child: Panel) {
        add(child)
    }

    fun add(child: Panel, weight: Float): PanelList {
        add(child)
        child.weight = weight
        return this
    }

    fun addChild(panel: Panel) {
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
            val available = (if (isY) h else w) - (children.size / 2) * spacing
            val sumWeight = SumOf.sumOf(children.filter { it !is CustomizingBar }) { max(minWeight, it.weight) }
            val weightScale = 1f / sumWeight

            var childPos = if (isY) y else x
            for (child in children) {
                val weight = max(minWeight, child.weight)
                val childSize = if (child is CustomizingBar)
                    if (isY) child.minH else child.minW
                else {
                    val betterWeight = max(weight * weightScale, minSize)
                    if (betterWeight != weight) child.weight = betterWeight
                    (betterWeight / sumWeight * available).roundToInt()
                }
                childPos += min(
                    childSize, if (isY) {
                        child.calculateSize(w, childSize)
                        child.place(x, childPos, w, childSize)
                        child.h
                    } else {
                        child.calculateSize(childSize, h)
                        child.place(childPos, y, childSize, h)
                        child.w
                    }
                )
            }
        }
    }

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
                    (vg as? CustomList)?.update()
                } else {
                    // todo remove the last child -> remove this from our parent
                    (parent as? CustomList)?.remove(indexInParent)
                }
            }
        }
    }


}