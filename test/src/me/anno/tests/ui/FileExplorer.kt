package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFXBase.disableRenderDoc
import me.anno.io.files.FileReference
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption

fun main() {
    disableRenderDoc()
    testUI {
        ECSRegistry.init()
        object : FileExplorer(null, DefaultConfig.style) {
            override fun getFolderOptions() = emptyList<FileExplorerOption>()
            override fun onDoubleClick(file: FileReference) {}
            override fun onPaste(x: Float, y: Float, data: String, type: String) {}
        }
    }
}