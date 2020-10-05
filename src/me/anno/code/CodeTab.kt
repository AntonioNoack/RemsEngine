package me.anno.code

import me.anno.ui.base.TextPanel
import me.anno.ui.style.Style
import java.io.File

class CodeTab(val file: File, style: Style): TextPanel(file.name, style) {
    init {
        setSimpleClickListener {
            CodeEditor.openFile(file)
        }
    }
}