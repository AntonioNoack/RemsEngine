package me.anno.tools

import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.mapNotNull
import me.anno.utils.structures.Iterators.toList

fun main() {
    val project = documents.getChild("IdeaProjects/RemsEngine")
    val src = project.getChild("src")
    val sources = listOf(
        src
    )
    val todos = sources.flatMap { findTodos(it) }
    val index = Maths.randomInt(0, todos.size)
    println("Picked $index/${todos.size}: '${todos[index]}'")
}

fun findTodos(file: FileReference): List<String> {
    if (file.isDirectory) {
        return file.listChildren().flatMap { child ->
            findTodos(child)
        }
    }

    if (file.lcExtension != "kt") return emptyList()
    val lines = file.readLinesSync(256)
    return lines
        .mapNotNull { line ->
            val prefix = "todo "
            val idx = line.indexOf(prefix)
            if (idx >= 0) line.substring(idx + prefix.length)
            else null
        }
        .toList()
}