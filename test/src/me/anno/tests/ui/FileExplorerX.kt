package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.files.FileReference
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption

fun main() {
    disableRenderDoc()
    testUI3("File Explorer X") {
        ECSRegistry.init()
        FileExplorer(null, false, style)
    }
}