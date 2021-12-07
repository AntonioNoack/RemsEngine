package me.anno.ui.editor.stacked

import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.style.Style

abstract class ArrayPanel<EntryType, PanelType : Panel>(
    title: String,
    visibilityKey: String,
    val newValue: () -> EntryType, style: Style
) : TitledListY(title, visibilityKey, style) {

    // todo add cross to remove entry (?)

    val content = ArrayList<EntryType>()

    // todo drag
    // todo add
    // todo remove
    // todo move around (like scene views)
    // todo ...

    // todo thousands of editing fields, or edit one at a time? idk...

    // todo a type of pair panel, so this can be expanded to be a hashmap-editor

    // abstract fun setValue(panel: PanelType, value: EntryType)
    abstract fun createPanel(value: EntryType): PanelType

    override fun clear() {
        super.clear()
        content.clear()
    }

    fun setValues(values: List<EntryType>) {
        clear()
        for (value in values) {
            add(createPanel(value))
        }
        content.addAll(values)
    }

    fun set(panel: Panel, value: Any?) {
        val index = children.indexOf(panel) - 1
        content[index] = value as EntryType
        onChange()
    }

    fun getValues(): List<EntryType> = content

    open fun onChange() {}

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            // todo when everything is working, use actions instead
            button.isRight -> {
                openMenu(listOf(
                    MenuOption(NameDesc("Add Entry")) {
                        val value = newValue()
                        content.add(value)
                        children.add(createPanel(value))
                        invalidateLayout()
                    }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

}