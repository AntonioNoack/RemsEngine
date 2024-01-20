package me.anno.tests.files

import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.documents

fun main() {
    var file = documents
    while (true) {
        val parent = file.getParent().nullIfUndefined() ?: break
        file = parent
    }
    println(file)
    println(file.listChildren())
    val sample = file.listChildren().first()
    println(sample.absolutePath)
    println(getReference(sample.absolutePath))
}