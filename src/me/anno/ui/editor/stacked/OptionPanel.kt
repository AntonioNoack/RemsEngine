package me.anno.ui.editor.stacked

import me.anno.input.MouseButton
import me.anno.io.Saveable
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.NameDesc
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
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
        setTooltip(tooltipText)
        PropertyInspector.createInspector(value, content, style)
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight -> {
                val index = indexInParent
                openMenu(
                    stackPanel.options.map { option ->
                        // todo translate
                        MenuOption(
                            NameDesc("Prepend %1", option.description, "ui.option.prepend").with(
                                "%1",
                                option.title
                            )
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
                    }
                )
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        stackPanel.removeComponent(value)
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return if (value is Saveable) {
            TextWriter.toText(value, false)
        } else {
            super.onCopyRequested(x, y)
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (data.isBlank2()) return
        try {
            val index = indexInParent
            val values = TextReader.read(data)
            for (it in values) {
                it as? Inspectable ?: continue
                val option = stackPanel.getOptionFromInspectable(it) ?: continue
                stackPanel.addComponent(option, index, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMultiSelectablePanel(): Panel? = this

}