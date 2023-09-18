package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.Style
import kotlin.math.max

/**
 * nine tile panel, which solves all constraints, and aligns the items appropriately
 * when there are multiple panels per tile, they will get stacked
 * */
open class NineTilePanel(style: Style) : PanelGroup(style) {

    constructor(base: NineTilePanel) : this(base.style) {
        base.copyInto(this)
    }

    override val children = ArrayList<Panel>()

    override fun remove(child: Panel) {
        children.remove(child)
    }

    fun add(panel: Panel) {
        children.add(panel)
        panel.uiParent?.remove(panel)
        panel.parent = this
    }

    fun add(index: Int, panel: Panel) {
        children.add(index, panel)
        panel.uiParent?.remove(panel)
        panel.parent = this
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int) = true

    var splitX = 0.2f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }
    var splitY = 0.2f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    private val minWHs = IntArray(8)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val sx = (splitX * w).toInt()
        val sy = (splitY * h).toInt()

        for (i in children.indices) {
            val child = children[i]
            val aw = when (child.alignmentX) {
                AxisAlignment.FILL -> w
                AxisAlignment.MIN, AxisAlignment.MAX -> sx
                AxisAlignment.CENTER -> w - 2 * sx
            }
            val ah = when (child.alignmentY) {
                AxisAlignment.FILL -> h
                AxisAlignment.MIN, AxisAlignment.MAX -> sy
                AxisAlignment.CENTER -> h - 2 * sy
            }
            child.calculateSize(aw, ah)
            val mixX = child.alignmentX != AxisAlignment.FILL
            val mixY = child.alignmentY != AxisAlignment.FILL
            child.width = if (mixX) mix(child.minW, aw, child.weight) else aw
            child.height = if (mixY) mix(child.minH, ah, child.weight2) else ah
            val sxi = child.alignmentX.id
            val syi = child.alignmentY.id + 4
            minWHs[sxi] = max(minWHs[sxi], child.width)
            minWHs[syi] = max(minWHs[syi], child.height)
        }

        minW = max(minWHs[3], max(minWHs[0], minWHs[2]) * 2 + minWHs[1])
        minH = max(minWHs[7], max(minWHs[4], minWHs[6]) * 2 + minWHs[5])

    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val children = children
        for (i in children.indices) {
            val child = children[i]
            val cx = x + child.alignmentX.getOffset(width, child.width)
            val cy = y + child.alignmentY.getOffset(height, child.height)
            child.setPosition(cx, cy)
        }
    }

    override fun clone() = NineTilePanel(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as NineTilePanel
        dst.children.clear()
        dst.children.addAll(children.map { it.clone() })
    }

    override val className: String get() = "NineTilePanel"

}