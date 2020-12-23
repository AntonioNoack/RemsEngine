package me.anno.ui.editor.stacked

import me.anno.gpu.GFX
import me.anno.gpu.GFX.openMenu
import me.anno.input.MouseButton
import me.anno.io.Saveable
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.SettingCategory

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
                        GFX.MenuOption("Prepend ${option.title}", option.description) {
                            stackPanel.addComponent(option, index, true)
                        }
                    } + GFX.MenuOption("Remove Component", "") {
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
        try {
            val index = indexInParent
            val values = TextReader.fromText(data)
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