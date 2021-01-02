package me.anno.objects.inspectable

import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

interface Inspectable {

    fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, id: String) -> SettingCategory)

}