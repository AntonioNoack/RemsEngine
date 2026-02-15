package me.anno.tests.utils

import me.anno.io.files.FileReference
import me.anno.utils.OS

val tasks = HashMap<String, String>()
fun main() {
    // find all tasks
    index(OS.engineProject)
    // choose random one
    println(tasks.entries.random())
}

fun index(file: FileReference) {
    if (file.isDirectory) {
        for (child in file.listChildren()) {
            index(child)
        }
    } else if (file.lcExtension == "kt") {
        for ((lineIndex, line) in file.readLinesSync(256).withIndex()) {
            val prefix = "// todo"
            val idx = line.indexOf(prefix)
            if (idx >= 0) {
                val task = line.substring(idx + prefix.length).trim()
                tasks[task] = "$file:${lineIndex + 1}"
            }
        }
    }
}

// todo fix dragging VectorInputs