package me.anno.ui.impl

import me.anno.ui.base.Panel
import me.anno.ui.base.ScrollPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.Exactly
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

class PropertyInspector(style: Style, padding: Padding):
    ScrollPanel(style.getChild("propertyInspector"), padding, WrapAlign.AxisAlignment.MIN){

    val list = child as PanelListY

    init {
        padding.top = 8
    }

    operator fun plusAssign(panel: Panel){
        list += panel
    }

}