package me.anno.experiments

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.pictures

fun main() {
    testUI3("Gallery", FileExplorer(pictures, true, style))
}