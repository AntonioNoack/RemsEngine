package me.anno.objects

import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

interface Inspectable {

    fun createInspector(list: PanelListY, style: Style)

}