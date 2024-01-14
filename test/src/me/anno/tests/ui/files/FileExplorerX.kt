package me.anno.tests.ui.files

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer

fun main() {
    disableRenderDoc()
    testUI3("File Explorer X") {
        ECSRegistry.init()
        FileExplorer(null, false, style)
    }
}