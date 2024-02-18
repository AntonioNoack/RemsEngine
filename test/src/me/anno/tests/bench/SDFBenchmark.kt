package me.anno.tests.bench

import me.anno.Time
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.fonts.Font
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

    OfficialExtensions.register()
    ExtensionLoader.load()

    val logger = LogManager.getLogger("SDFBenchmark")

    val roundEdges = false
    val font = Font("Verdana", 8f, isBold = false, isItalic = false)
    val text = "Lorem Ipsum is simply text."

    val t0 = Time.nanoTime
    val data = SignedDistanceField.createBuffer(font, text, roundEdges)!!
    val t1 = Time.nanoTime
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