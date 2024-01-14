package me.anno.tests.ui.files

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.documents
import me.anno.utils.files.FileChooser
import me.anno.utils.files.FileExtensionFilter

/**
 * tests the file chooser
 * */
fun main() {
    // todo scrollbar is gone, scrolling is impossible
    RenderDoc.disableRenderDoc()
    testUI3("FileChooserSave") {
        FileChooser.createFileChooserUI(
            true, false, true, true,
            documents, listOf(
                FileExtensionFilter(NameDesc("Everything"), emptyList()),
                FileExtensionFilter(NameDesc("Images"), listOf("png", "jpg", "webp", "svg")),
                FileExtensionFilter(NameDesc("PNGs"), listOf("png")),
                FileExtensionFilter(NameDesc("PDFs"), listOf("pdf")),
                FileExtensionFilter(NameDesc("SVGs"), listOf("svg")),
            ), style
        ) {
            println("Selected $it")
        }
    }
}