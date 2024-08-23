package me.anno.engine.ui.input

import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.editor.stacked.ArrayPanel
import me.anno.utils.structures.Pointer
import me.anno.utils.types.Defaults

/**
 * UI for editing arrays
 * */
open class AnyArrayPanel(nameDesc: NameDesc, visibilityKey: String, val childType: String, style: Style) :
    ArrayPanel<Any?, Panel>(nameDesc, visibilityKey, { Defaults.getDefaultValue(childType) }, style) {

    override fun createPanel(value: Any?): Panel {
        val panel = Pointer<Panel?>(null)
        val property = ArrayPanelProperty(this, value, childType, panel)
        panel.value = ComponentUI.createUIByTypeName("", visibilityKey, property, childType, null, style)
        return panel.value!!
    }
}