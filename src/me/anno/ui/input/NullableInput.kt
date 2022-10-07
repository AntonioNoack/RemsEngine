package me.anno.ui.input

import me.anno.engine.IProperty
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style

class NullableInput<V>(val content: Panel, val property: IProperty<V>, style: Style) :
    PanelListX(style), InputPanel<V> {

    val value0 = property.get()
    val default = property.getDefault()

    init {
        add(content)
    }

    val checkbox = Checkbox(
        value0 != null, default != null,
        style.getSize("fontSize", 10), style
    )

    init {
        add(checkbox.apply {
            tooltip = "Toggle Null"
            @Suppress("unchecked_cast")
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

    override var isInputAllowed
        get() = checkbox.isInputAllowed && (content !is InputPanel<*> || content.isInputAllowed)
        set(value) {
            checkbox.isInputAllowed = value
            (content as? InputPanel<*>)?.isInputAllowed = value
        }

    override val lastValue: V
        get() = property.get()

    override fun setValue(value: V, notify: Boolean): Panel {
        property.set(this, value)
        return this
    }

    var changeListener = {}
    var resetListener = {}

}