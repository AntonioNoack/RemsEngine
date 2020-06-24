package me.anno.ui.editor

import me.anno.gpu.GFX
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.ui.base.Panel
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

class PropertyInspector(style: Style):
    ScrollPanelY(style.getChild("propertyInspector"), Padding(3), AxisAlignment.MIN){

    val list = child as PanelListY
    var lastSelected: Inspectable? = null

    // init { padding.top += 6 }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val selected = GFX.selectedInspectable
        if(selected != lastSelected){
            lastSelected = selected
            list.clear()
            selected?.createInspector(list, style)
        }
        super.draw(x0, y0, x1, y1)
    }

    operator fun plusAssign(panel: Panel){
        list += panel
    }

}