package me.anno.utils.bench

import me.anno.fonts.FontManager
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.utils.Maths.sq
import me.anno.utils.OS
import java.awt.Font
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.FloatBuffer


fun toBytes(data: FloatBuffer): ByteArray {
    val bos = ByteArrayOutputStream()
    val output = DataOutputStream(bos)
    for (index in 0 until data.capacity()) {
        output.writeByte(data[index].toInt())
    }
    output.flush()
    return bos.toByteArray()
}

fun main() {

    val roundEdges = false
    val font = FontManager.getFont("Verdana", 8f, false, false).font
    val text = "Lorem Ipsum is simply text."

    Thread.sleep(8000)

    val t0 = System.nanoTime()

    val data = SignedDistanceField.createBuffer(font, text, roundEdges)!!

    val t1 = System.nanoTime()
    println("Used ${((t1-t0)*1e-9)}s")

    val calculated = toBytes(data)

    val file = File(OS.desktop.file, "sdf.data")
    if (file.exists()) {
        val bytes = file.readBytes()
        val sum = calculated.withIndex().sumOf { (index, value) -> sq(value-bytes[index]) }
        println("Error: $sum, ${sum.toDouble()/calculated.size}")
    } else {
        file.writeBytes(calculated)
    }

}