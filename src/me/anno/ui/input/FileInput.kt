package me.anno.ui.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.FileExplorerSelect
import me.anno.utils.FileHelper.openInExplorer
import java.io.File
import kotlin.concurrent.thread

class FileInput(title: String, style: Style, f0: File, val isDirectory: Boolean = false) : PanelListX(style) {

    val button = TextButton(DefaultConfig["ui.symbol.folder", "\uD83D\uDCC1"], true, style)
    val base = TextInput(title, false, style, f0.toString())
    val base2 = base.base

    val text get() = base.text

    init {
        setTooltip(title)
        base.apply {
            this += WrapAlign.LeftCenter
        }
        button.apply {
            setSimpleClickListener {
                thread {
                    FileExplorerSelect.selectFileOrFolder(file, isDirectory) { file ->
                        if (file != null) {
                            changeListener(file)
                            base.setText(file.toString(), false)
                        }
                    }
                }
            }
            setTooltip("Select the file in your default file explorer")
            textColor = textColor and 0x7fffffff
            focusTextColor = textColor
        }
        this += button
        // for a symmetric border
        val border = style.getPadding("borderSize", 2).left
        if(border > 0) this += SpacePanel(border, 0, style).apply { backgroundColor = 0 }
        this += base
    }

    val file get() = File(base.text)

    var changeListener = { _: File -> }
    fun setChangeListener(listener: (File) -> Unit): FileInput {
        this.changeListener = listener
        return this
    }

    fun setText(text: String, notify: Boolean) {
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