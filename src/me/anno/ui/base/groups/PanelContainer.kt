package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style

open class PanelContainer(val child: Panel, val padding: Padding, style: Style): PanelGroup(style) {

    init { child.parent = this }

    override val children: List<Panel> = listOf(child)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(w - padding.width, h - padding.height)
        child.applyConstraints()
        minW = child.minW + padding.width
        minH = child.minH + padding.height
        applyConstraints()
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        child.placeInParent(x + padding.left, y + padding.right)
    }

    override fun getClassName(): String = "PanelContainer"

}