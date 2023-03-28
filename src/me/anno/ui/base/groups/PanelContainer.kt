package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style

open class PanelContainer(
    onlyChild: Panel,
    val padding: Padding,
    style: Style
) : PanelGroup(style) {

    constructor(base: PanelContainer) : this(base.child.clone(), base.padding, base.style) {
        base.copyInto(this)
    }

    init {
        onlyChild.parent = this
    }

    var child: Panel = onlyChild
        set(value) {
            if (field != value) {
                field.parent = null
                field = value
                value.uiParent?.remove(value)
                value.parent = this
                children.clear()
                children.add(value)
                invalidateLayout()
            }
        }

    override val children = arrayListOf(child)

    override fun remove(child: Panel) {
        children.remove(child)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(w - padding.width, h - padding.height)
        minW = child.minW + padding.width
        minH = child.minH + padding.height
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        child.setPosition(x + padding.left, y + padding.top)
    }

    override fun clone() = PanelContainer(this)

    companion object {
        const val maxLength = 2_000_000_000 // max value, but also enough for any padding addition/subtraction
    }

}