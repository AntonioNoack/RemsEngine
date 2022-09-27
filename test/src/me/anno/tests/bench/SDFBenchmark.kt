package me.anno.tests.bench

import me.anno.fonts.FontManager
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.maths.Maths.sq
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
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

    val logger = LogManager.getLogger("SDFBenchmark")

    val roundEdges = false
    val font = FontManager.getFont("Verdana", 8f, bold = false, italic = false).font
    val text = "Lorem Ipsum is simply text."

    Thread.sleep(8000)

    val t0 = System.nanoTime()

    val data = SignedDistanceField.createBuffer(font, text, roundEdges)!!

    val t1 = System.nanoTime()
    logger.info("Used ${((t1 - t0) * 1e-9)}s")

    val calculated = toBytes(data)

    val file = OS.desktop.getChild("sdf.data")
    if (file.exists) {
        val bytes = file.readBytesSync()
        val sum = calculated.withIndex().sumOf { (index, value) -> sq(value - bytes[index]) }
        logger.info("Error: $sum, ${sum.toDouble() / calculated.size}")
    } else {
        file.writeBytes(calculated)
    }

}