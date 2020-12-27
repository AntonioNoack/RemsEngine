package me.anno.studio.rems

import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

object ProjectSettings : Transform() {

    override fun getDefaultDisplayName(): String = "Project Settings"

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {

        val project = project!!
        list += vi("Language", "For Spellchecking", null, project.language, style){ project.language = it; save() }

    }

    fun save() = RenderSettings.save()

}