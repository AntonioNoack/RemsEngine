package me.anno.ui.editor.stacked

import me.anno.input.Key
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.engine.inspector.Inspectable
import me.anno.engine.EngineBase
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.SettingCategory
import me.anno.utils.types.Strings.isBlank2

class OptionPanel(
    private val stackPanel: StackPanel,
    title: String,
    tooltipText: String,
    val value: Inspectable
) : SettingCategory(title, stackPanel.style) {

    init {
        tooltip = tooltipText
        PropertyInspector.createInspector(value, content, style)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_RIGHT -> {
                val index = indexInParent
                openMenu(windowStack, stackPanel.options.map { option ->
                    MenuOption(
                        NameDesc("Prepend %1", option.description, "ui.option.prepend")
                            .with("%1", option.title)
                    ) {
                        stackPanel.addComponent(option, index, true)
                    }
                } + MenuOption(
                    NameDesc(
                        "Remove Component",
                        "Deletes the component",
                        "ui.general.removeComponent"
                    )
                ) {
                    stackPanel.removeComponent(value)
                })
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        stackPanel.removeComponent(value)
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return if (value is Saveable) {
            JsonStringWriter.toText(value, InvalidRef)
        } else super.onCopyRequested(x, y)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (data.isBlank2()) return
        try {
            val index = indexInParent
            val values = JsonStringReader.read(data, EngineBase.workspace, true)
            for (it in values) {
                it as? Inspectable ?: continue
                val option = stackPanel.getOptionFromInspectable(it) ?: continue
                stackPanel.addComponent(option, index, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMultiSelectablePanel() = this
}