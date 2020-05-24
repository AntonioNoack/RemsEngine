package me.anno.ui.impl.explorer

import me.anno.ui.base.TextPanel
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import java.io.File

class FileEntry(val file: File, style: Style): TextPanel(file.name, style){

    override fun printLayout(depth: Int) {
        super.printLayout(depth)
        println("${Tabs.spaces(depth*2+2)} ${file.name}")
    }

}