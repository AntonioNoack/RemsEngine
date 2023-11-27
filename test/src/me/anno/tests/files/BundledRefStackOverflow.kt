package me.anno.tests.files

import me.anno.io.files.BundledRef

fun main() {
    // can't really reproduce it :/
    BundledRef.parse("res://test\\xs//hello.js").readTextSync()
}