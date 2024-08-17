package me.anno.tests.ui.files

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.documents

fun main() {
    // done copy a file with Ctrl+C/V -> needs "Copy" to be selected in the menu
    // todo cut and paste a file with Ctrl+X/V
    // done move a file by dragging it
    val test = documents.getChild("Test")
    test.tryMkdirs()
    test.listChildren().forEach { it.delete() }
    test.getChild("1. Copy Me, 1B.txt").writeText("1")
    test.getChild("2. Cut Me, 7B.txt").writeText("1234567")
    test.getChild("3. Drag Me, 5B.txt").writeText("12345")
    test.getChild("Drag Target").tryMkdirs()
    testUI("Files: Copy/Paste") {
        FileExplorer(test, true, style)
    }
}