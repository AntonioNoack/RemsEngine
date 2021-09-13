package me.anno.utils.test.files

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER

fun main() {

    val file = getReference("C:\\Users\\Antonio\\Pictures\\RemsStudio/zip.7z")
    LOGGER.info(file.listChildren())

}