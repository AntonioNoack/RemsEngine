package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.tests.LOGGER
import me.anno.utils.structures.Iterators.count
import me.anno.utils.types.Strings.isBlank2

fun main() {
    val file = getReference("C:\\Users\\Antonio\\Documents\\IdeaProjects\\RemsEngine\\src\\me\\anno")
    LOGGER.info(countLines(file))
}

fun countLines(file: FileReference): Int {
    return if (file.isDirectory) {
        file.listChildren().sumOf { countLines(it) }
    } else when (file.lcExtension) {
        "kt", "java" -> {
            file.readLinesSync(64).count {
                !it.isBlank2() && !it.trim().startsWith("//")
            }
        }
        else -> 0
    }
}