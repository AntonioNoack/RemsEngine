package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style
import me.anno.utils.structures.lists.UpdatingSingletonList

open class PanelContainer(
    onlyChild: Panel,
    val padding: Padding,
    style: Style
) : PanelGroup(style) {

    init {
        onlyChild.parent = this
    }

    var child: Panel = onlyChild
        set(value) {
            if (field != value) {
                child.parent = null
                value.parent?.remove(value)
                value.parent = this
                field = value
                invalidateLayout()
            }
        }

    override val children: List<Panel> = UpdatingSingletonList { child }

    override fun remove(child: Panel) {
        this.child = Panel(style)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(w - padding.width, h - padding.height)
        minW = child.minW + padding.width
        minH = child.minH + padding.height
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        child.placeInParent(x + padding.left, y + padding.top)
    }

    companion object {
        const val maxLength = 2_000_000_000 // max value, but also enough for any padding addition/subtraction
    }

}