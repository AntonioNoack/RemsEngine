package me.anno.tests.io

import me.anno.io.files.Reference

fun main() {
    val javaClass = Reference.javaClass
    println(javaClass.classLoader.getResourceAsStream("/saveables.yaml")) // incorrect
    println(javaClass.classLoader.getResourceAsStream("saveables.yaml")) // correct
}