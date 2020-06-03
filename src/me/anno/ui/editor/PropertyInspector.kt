package me.anno.ui.editor

import me.anno.gpu.GFX
import me.anno.objects.Transform
import me.anno.ui.base.Panel
import me.anno.ui.base.ScrollPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

class PropertyInspector(style: Style):
    ScrollPanel(style.getChild("propertyInspector"), Padding(3), WrapAlign.AxisAlignment.MIN){

    val list = child as PanelListY
    var lastSelected: Transform? = null

    init { padding.top += 6 }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val selected = GFX.selectedTransform
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