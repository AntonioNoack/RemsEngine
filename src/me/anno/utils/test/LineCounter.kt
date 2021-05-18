package me.anno.utils.test

import java.io.File
import java.util.*

fun main(){
    println(
        countLines(
            File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\VideoStudio\\src\\me\\anno")
        )
    )
}

fun countLines(file: File): Int {
    return if (file.isDirectory) {
        file.listFiles()?.sumOf { countLines(it) } ?: 0
    } else when (file.extension.lowercase(Locale.getDefault())) {
        "kt", "java" -> {
            file.readLines().size
        }
        else -> 0
    }
}