package me.anno.ui.input

import me.anno.engine.IProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style

class NullableInput(val content: Panel, val property: IProperty<*>, style: Style) : PanelListX(style) {

    init {
        val value0 = property.get()
        val default = property.getDefault()
        add(content)
        val checkbox = Checkbox(
            value0 != null, default != null,
            style.getSize("fontSize", 10), style
        )
        add(checkbox.apply {
            tooltip = "Toggle Null"
            @Suppress("UNCHECKED_CAST")
            setChangeListener { isNotNull ->
                property as IProperty<Any?>
                content as InputPanel<Any?>
                content.isEnabled = isNotNull
                if (isNotNull) {
                    content.setValue(content.lastValue, true)
                } else {
                    property.set(content, null)
                }
            }
        })
    }

    var changeListener = {}
    var resetListener = {}

}