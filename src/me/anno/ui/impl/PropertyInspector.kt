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

    init {
        this += Exactly(style.getSize("textSize", 12) * 20, null)
    }

    val list = child as PanelListY
    operator fun plusAssign(panel: Panel){
        list += panel
    }

}