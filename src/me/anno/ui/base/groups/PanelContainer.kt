package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style

open class PanelContainer(onlyChild: Panel, val padding: Padding, style: Style): PanelGroup(style) {

    init { onlyChild.parent = this }

    var child: Panel = onlyChild
        set(value) {
            child.parent = null
            value.parent?.remove(value)
            value.parent = this
            field = value
        }

    override val children: List<Panel> get() = listOf(child)

    override fun remove(child: Panel) {
        this.child = Panel(style)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(w - padding.width, h - padding.height)
        // child.applyConstraints()
        minW = child.minW + padding.width
        minH = child.minH + padding.height
        // applyConstraints()
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        child.placeInParent(x + padding.left, y + padding.right)
    }

    override fun getClassName(): String = "PanelContainer"

}