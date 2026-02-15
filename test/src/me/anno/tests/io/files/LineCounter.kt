package me.anno.tests.io.files

import me.anno.io.files.FileReference
import me.anno.tests.LOGGER
import me.anno.utils.OS
import me.anno.utils.structures.Iterators.toList
import me.anno.utils.types.Floats.formatPercent
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.spaces
import kotlin.math.min

// todo have 10% docs everywhere(?)
//  never ever write useless/redundant docs!

val project = OS.engineProject
const val threshold = 10_000

class LocalTotal(val partial: Int, val total: Int, val docs: Int) {
    operator fun plus(other: LocalTotal): LocalTotal {
        return LocalTotal(partial + other.partial, total + other.total, docs + other.docs)
    }
}

val zero = LocalTotal(0, 0, 0)

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

fun printResult(sp: Int, relPath: String, sum: LocalTotal) {
    val core = if (sum.partial == sum.total) {
        "${sum.total}"
    } else {
        "${sum.partial}/${sum.total}"
    }
    LOGGER.info("${spaces(sp)}$relPath: $core, docs: ${formatPercent(sum.docs, sum.total)}%")
}

fun countLines(file: FileReference, root: Int = file.toString().length + 1): LocalTotal {
    return if (file.isDirectory) {
        // handle folder
        val absPath = file.toString()
        val children = file.listChildren()
        val sum = children
            .map { countLines(it, root) }
            .reduceOrNull { a, b -> a + b }
            ?: zero
        if (absPath.length <= root && sum.total > 0) {
            val relPath = absPath.substring(project.toString().length + 1)
            printResult(0, relPath, sum)
        }
        if (sum.partial > threshold) {
            if (absPath.length > root) {
                val relPath = absPath.substring(min(root, absPath.length))
                printResult(2, relPath, sum)
            }
            LocalTotal(0, sum.total, sum.docs)
        } else sum
    } else when (file.lcExtension) {
        "kt", "java" -> { // handle source file
            val lines = file.readLinesSync(64).toList().map { it.trim() }
            val sum = lines.count {
                !it.isBlank2() && !it.startsWith("//")
            }
            val docs = lines.count {
                it.startsWith("//") || it.startsWith("/*") || it.startsWith("* ")
            }
            LocalTotal(sum, sum, docs)
        }
        else -> zero
    }
}