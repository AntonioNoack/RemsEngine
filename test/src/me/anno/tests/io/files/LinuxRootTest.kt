package me.anno.tests.io.files

import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS
import me.anno.utils.assertions.assertTrue

fun main() {
    val root = getReference("/")
    println(FileRootRef.absolutePath)
    println(FileRootRef.listChildren())
    println(root.listChildren())
    assertTrue(root.isDirectory)
    var file = OS.documents
    while (file != InvalidRef) {
        println(file)
        file = file.getParent()
    }

    println("\nLinux folders")
    println(getReference("/mnt").listChildren())
    println(getReference("/media").listChildren())
    println(getReference("/media").isDirectory)
    println(getReference("/media/antonio").listChildren())
    println(getReference("/media/antonio/58CE075ECE0733B2").listChildren())

    println("\nNormalized folders")
    println(getReference("mnt").listChildren())
    println(getReference("media").listChildren())
    println(getReference("media").isDirectory)
    println(getReference("media/antonio").listChildren())
    println(getReference("media/antonio/58CE075ECE0733B2").listChildren())
}