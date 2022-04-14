package me.anno.utils.test.files

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER
import me.anno.utils.types.Strings.isBlank2

fun main() {
    val file = getReference("C:\\Users\\Antonio\\Documents\\IdeaProjects\\VideoStudio\\src\\me\\anno")
    LOGGER.info(countLines(file))
}

fun countLines(file: FileReference): Int {
    return if (file.isDirectory) {
        file.listChildren()?.sumOf { countLines(it) } ?: 0
    } else when (file.extension.lowercase()) {
        "kt", "java" -> {
            file.readLines().count {
                !it.isBlank2() && !it.trim().startsWith("//")
            }
        }
        else -> 0
    }
}