package me.anno.tests.mesh.vox

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.downloads

fun main() {
    ECSRegistry.init()
    val ui = FileExplorer(downloads.getChild("MagicaVoxel/vox"), true, style)
    testUI3("Vox", ui)
}