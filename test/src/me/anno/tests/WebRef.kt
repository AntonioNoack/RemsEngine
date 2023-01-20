package me.anno.tests

import me.anno.io.files.FileReference
import me.anno.io.files.WebRef

fun main() {
    val file = FileReference.getReference("https://www.google.com/search?q=search&oq=search&sourceid=chrome&ie=UTF-8")
    println(file.exists)
    println(file.lastModified)
    println(file.length())
    println(WebRef.getHeaders(file.toUri().toURL(), 1000L, false))
    println((file as WebRef).responseCode)
    println(file.path)
    println(file.arguments)
    println(file.hashbang)
}