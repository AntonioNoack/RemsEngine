package me.anno.engine.ui

import me.anno.engine.Ptr
import me.anno.ui.base.Panel
import me.anno.ui.editor.stacked.ArrayPanel
import me.anno.ui.style.Style

open class AnyArrayPanel(title: String, visibilityKey: String, val arrayType: String, style: Style) :
    ArrayPanel<Any?, Panel>(title, visibilityKey, { ComponentUI.getDefault(arrayType) }, style) {

    override fun createPanel(value: Any?): Panel {
        val panel = Ptr<Panel?>(null)
        val property = AAPProperty(this, value, arrayType, panel)
        panel.value = ComponentUI.createUIByTypeName("", visibilityKey, property, arrayType, null, style)
        return panel.value!!
    }

}