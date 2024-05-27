package me.anno.engine.inspector

import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory

interface Inspectable {
    fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ): Unit = createInspector(listOf(this), list, style, getGroup)

    fun createInspector(
        inspected: List<Inspectable>, list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ): Unit = AutoInspector.inspect(inspected, list, style)

    fun createInspector(list: PanelListY, style: Style): Unit =
        createInspector(listOf(this), list, style)

    fun createInspector(inspected: List<Inspectable>, list: PanelListY, style: Style) {
        val groups = HashMap<NameDesc, SettingCategory>()
        createInspector(inspected, list, style) {
            groups.getOrPut(it) { SettingCategory(it, style) }
        }
    }
}