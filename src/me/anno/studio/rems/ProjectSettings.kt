package me.anno.studio.rems

import me.anno.language.Language
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

object ProjectSettings : Transform() {

    override fun getDefaultDisplayName(): String = "Project Settings"

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list.add(createSpellcheckingPanel(style))
    }

    fun createSpellcheckingPanel(style: Style): Panel {
        val project = project!!
        val name = NameDesc("Language", "For Spellchecking", "")
        val values = Language.values()
        return EnumInput(name, project.language.naming, values.map { it.naming }, style)
            .setChangeListener { _, index, _ ->
                project.language = values[index]; save()
            }
    }

    fun save() = RenderSettings.save()

}