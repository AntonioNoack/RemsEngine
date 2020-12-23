package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.style.Style
import me.anno.utils.FileHelper.openInExplorer
import java.io.File

class FileInput(title: String, style: Style) : TextInput(title, style) {

    // todo a button to choose a file?
    // right click + open in explorer + dragging the solution should be simple...

    constructor(title: String, style: Style, f0: File) : this(title, style) {
        setText(f0.toString(), false)
        setTooltip(title)
    }

    val file get() = File(text)

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isRight) {
            GFX.openMenu(listOf(
                GFX.MenuOption("Open In Explorer", "") { File(text).openInExplorer() }
            ))
        } else {
            super.onMouseClicked(x, y, button, long)
        }
    }

}