package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha

open class PanelContainer(
    onlyChild: Panel,
    val padding: Padding,
    style: Style
) : PanelGroup(style) {

    init {
        onlyChild.parent = this
    }

    var shadowRadius = style.getSize("shadowRadius", 15)
    var shadowColor = style.getColor("shadowColor", black.withAlpha(0.12f))
    var alwaysShowShadowX = false
    var alwaysShowShadowY = false

    var child: Panel = onlyChild
        set(value) {
            if (field != value) {
                field.parent = null
                field = value
                value.uiParent?.remove(value)
                value.parent = this
                children = listOf(value)
                invalidateLayout()
            }
        }

    override var children = listOf(child)

    override fun remove(child: Panel) {
        if (child === this.child) children = emptyList()
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(w - padding.width, h - padding.height)
        minW = child.minW + padding.width
        minH = child.minH + padding.height
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val wi = width - padding.width
        val hi = height - padding.height
        child.setPosition(
            x + padding.left + child.alignmentX.getOffset(wi, child.minW),
            y + padding.top + child.alignmentY.getOffset(hi, child.minH),
        )
    }

    override fun setSize(w: Int, h: Int) {
        super.setSize(w, h)
        val wi = width - padding.width
        val hi = height - padding.height
        child.setSize(
            child.alignmentX.getSize(wi, child.minW),
            child.alignmentY.getSize(hi, child.minH)
        )
    }

    override fun clone(): PanelContainer {
        val clone = PanelContainer(child.clone(), padding.clone(), style)
        copyInto(clone)
        return clone
    }

    companion object {
        const val maxLength = 2_000_000_000 // max value, but also enough for any padding addition/subtraction
    }
}