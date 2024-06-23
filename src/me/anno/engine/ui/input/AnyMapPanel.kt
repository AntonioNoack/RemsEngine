package me.anno.engine.ui.input

import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.stacked.ArrayPanel
import me.anno.ui.input.InputVisibility
import me.anno.utils.structures.Pointer
import me.anno.utils.structures.tuples.MutablePair
import me.anno.utils.types.Defaults

// todo sort list/map by key or property of the users choice

/**
 * UI for editing maps
 * */
open class AnyMapPanel(
    title: String,
    visibilityKey: String,
    val keyType: String,
    val valueType: String,
    style: Style
) : ArrayPanel<MutablePair<Any?, Any?>, Panel>(title, visibilityKey, {
    MutablePair(Defaults.getDefaultValue(keyType), Defaults.getDefaultValue(valueType))
}, style) {

    override fun createPanel(value: MutablePair<Any?, Any?>): Panel {

        val keyPanelPtr = Pointer<Panel?>(null)
        val keyProperty = MapPanelProperty(value.first, { value.first = it; onChange() }, keyType, keyPanelPtr)
        val keyPanel = ComponentUI.createUIByTypeName("", "", keyProperty, keyType, null, style)
        keyPanel.setTooltip("Key")
        keyPanel.weight = 1f
        keyPanelPtr.value = keyPanel

        val valuePanelPtr = Pointer<Panel?>(null)
        val valueProperty = MapPanelProperty(value.second, { value.second = it; onChange() }, valueType, valuePanelPtr)
        val valuePanel = ComponentUI.createUIByTypeName("", "", valueProperty, valueType, null, style)
        valuePanel.setTooltip("Value")
        valuePanel.weight = Maths.GOLDEN_RATIOf
        valuePanelPtr.value = valuePanel

        val list = object : PanelListX(style) {
            override var isEnabled: Boolean
                get() = InputVisibility[visibilityKey]
                set(_) {}
        }
        list.add(keyPanelPtr.value!!)
        list.add(valuePanelPtr.value!!)
        return list
    }
}