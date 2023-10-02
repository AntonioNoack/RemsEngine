package me.anno.tests.ui

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.utils.files.FileChooser
import me.anno.utils.files.FileExtensionFilter

fun main() {
    val imageFilter = FileExtensionFilter(NameDesc("Images"), listOf("png", "jpg", "webp"))
    FileChooser.selectFiles(
        true, false, true,
        InvalidRef, true, listOf(imageFilter)
    ) {
        println("Selected $it")
    }
}