package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.image.raw.IntImage
import me.anno.io.files.inner.InnerFolder
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.desktop

fun main() {
    // todo file explorer seems to be lagging from Thumbnails, but also from text rendering -> eliminate that
    val format = "Grand Theft Auto V XXXX-XX-XX XX-XX-XX.mp4"
    val folder = InnerFolder(desktop.getChild("tmp"))
    val image = IntImage(2, 2, intArrayOf(1, 2, -1, 0), false)
    for (i in 0 until 10_000) {
        folder.createImageChild(
            format
                .map {
                    if (it == 'X') "0123456789".random()
                    else it
                }.joinToString(""), image
        )
    }
    testUI3("FileExplorerLag", FileExplorer(folder, true, style))
}