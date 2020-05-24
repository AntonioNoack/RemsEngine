package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.style.Style

open class PanelList(val sorter: Comparator<Panel>?, style: Style): PanelGroup(style){

    override val children = ArrayList<Panel>()
    var spacing = style.getSize("spacerWidth", 1)

    fun clear() = children.clear()

    operator fun plusAssign(panel: Panel){
        children += panel
        panel.parent = this
    }

    fun add(child: Panel): PanelList {
        this += child
        return this
    }

    override fun calculateSize(w: Int, h: Int) {
        if(sorter != null){
            children.sortWith(sorter)
        }
        super.calculateSize(w, h)
    }

}