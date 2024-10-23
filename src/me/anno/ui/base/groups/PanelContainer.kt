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

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        setPosSizeWithPadding(child, x, y, width, height, padding)
    }

    override fun clone(): PanelContainer {
        val clone = PanelContainer(child.clone(), padding.clone(), style)
        copyInto(clone)
        return clone
    }

    companion object {

        fun Panel.withPadding(l: Int, t: Int, r: Int, b: Int) = PanelContainer(this, Padding(l, t, r, b), style)

        const val MAX_LENGTH = 2_000_000_000 // max value, but also enough for any padding addition/subtraction

        fun setPosSizeWithPadding(child: Panel, x: Int, y: Int, width: Int, height: Int, padding: Padding) {
            child.setPosSizeAligned(
                x + padding.left,
                y + padding.top,
                width - padding.width,
                height - padding.height
            )
        }
    }
}