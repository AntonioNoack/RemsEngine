package me.anno.utils.test

import me.anno.io.files.FileReference.Companion.getReference

fun main() {

    val file = getReference("C:\\Users\\Antonio\\Pictures\\RemsStudio/zip.7z")
    println(file.listChildren())

}