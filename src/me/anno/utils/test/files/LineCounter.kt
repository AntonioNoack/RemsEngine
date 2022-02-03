package me.anno.utils.test.files

import me.anno.utils.LOGGER
import me.anno.utils.types.Strings.isBlank2
import java.io.File

fun main() {
    LOGGER.info(
        countLines(
            File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\VideoStudio\\src\\me\\anno")
        )
    )
}

fun countLines(file: File): Int {
    return if (file.isDirectory) {
        file.listFiles()?.sumOf { countLines(it) } ?: 0
    } else when (file.extension.lowercase()) {
        "kt", "java" -> {
            file.readLines().count {
                !it.isBlank2() && !it.trim().startsWith("//")
            }
        }
        else -> 0
    }
}