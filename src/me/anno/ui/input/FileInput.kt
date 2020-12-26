package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.FileExplorerSelect
import me.anno.utils.FileHelper.openInExplorer
import java.io.File
import kotlin.concurrent.thread

// class FileInput(title: String, style: Style) : TextInput(title, style) {
class FileInput(title: String, style: Style, f0: File, val isDirectory: Boolean = false) : PanelListX(style) {

    val button = TextButton("\uD83D\uDCC1", true, style)
    val base = TextInput(title, style, f0.toString())
    val base2 = base.base

    val text get() = base.text

    init {
        setTooltip(title)
        button.setSimpleClickListener {
            thread {
                FileExplorerSelect.selectFileOrFolder(file, isDirectory) { file ->
                    if (file != null) {
                        changeListener(file)
                        base.setText(file.toString(), false)
                    }
                }
            }
        }
        button.setTooltip("Select the file in your default file explorer")
        this += button
        this += base
        base += WrapAlign.LeftCenter
        button.textColor = button.textColor and 0x7fffffff
        button.focusTextColor = button.textColor
    }

    val file get() = File(base.text)

    var changeListener = { _: File -> }
    fun setChangeListener(listener: (File) -> Unit): FileInput {
        this.changeListener = listener
        return this
    }

    fun setText(text: String, notify: Boolean){
        base.setText(text, notify)
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight -> {
                GFX.openMenu(listOf(
                    GFX.MenuOption("Open In Explorer", "") { File(base.text).openInExplorer() }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun setIsSelectedListener(listener: () -> Unit): FileInput {
        base.setIsSelectedListener(listener)
        return this
    }

}