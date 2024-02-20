package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.tests.LOGGER
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.count
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.min

val project = documents.getChild("IdeaProjects/RemsEngine")
const val threshold = 10_000

/**
 * this method prints, which folders could be extracted into their own modules by line count length
 * */
fun main() {
    for (child in project.listChildren()) {
        if (child.name != "out") {
            countLines(child)
        }
    }
}

fun countLines(file: FileReference, root: Int = file.toString().length + 1): IntPair {
    return if (file.isDirectory) {
        val absPath = file.toString()
        val children = file.listChildren()
        val sum = children
            .map { countLines(it, root) }
            .reduceOrNull { a, b -> IntPair(a.first + b.first, a.second + b.second) }
            ?: IntPair(0, 0)
        if (absPath.length <= root && sum.second > 0) {
            val relPath = absPath.substring(project.toString().length + 1)
            LOGGER.info("$relPath: ${sum.second}")
        }
        if (sum.first > threshold) {
            if (absPath.length > root) {
                val relPath = absPath.substring(min(root, absPath.length))
                if (sum.first == sum.second) {
                    LOGGER.info("  $relPath: ${sum.first}")
                } else {
                    LOGGER.info("  $relPath: ${sum.first}/${sum.second}")
                }
            }
            IntPair(0, sum.second)
        } else sum
    } else when (file.lcExtension) {
        "kt", "java" -> {
            val sum = file.readLinesSync(64).count {
                !it.isBlank2() && !it.trim().startsWith("//")
            }
            IntPair(sum, sum)
        }
        else -> IntPair(0, 0)
    }
}