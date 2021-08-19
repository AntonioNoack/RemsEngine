package me.anno.utils.test

import me.anno.io.text.TextWriter

fun main() {

    val writer = TextWriter(false)
    writer.writeFloatArray2D("x", Array(5) { FloatArray(3) { it.toFloat() } })
    println(writer.data.toString())

}