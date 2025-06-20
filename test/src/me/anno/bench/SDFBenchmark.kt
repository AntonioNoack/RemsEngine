package me.anno.bench

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.maths.Maths.sq
import me.anno.utils.Clock
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

fun toBytes(data: FloatArray): ByteArray {
    val bos = ByteArrayOutputStream()
    val output = DataOutputStream(bos)
    for (element in data) {
        output.writeByte(element.toInt())
    }
    output.flush()
    return bos.toByteArray()
}

fun main() {

    OfficialExtensions.initForTests()

    val logger = LogManager.getLogger("SDFBenchmark")

    val roundEdges = false
    val font = Font("Verdana", 8f, isBold = false, isItalic = false)
    val text = "Lorem Ipsum is simply text."

    val clock = Clock("SDFBenchmark")
    val data = SignedDistanceField.createBuffer(font, text, roundEdges)!!
    clock.stop("SDF.createBuffer")

    val calculated = toBytes(data)

    val file = OS.desktop.getChild("sdf.bin")
    if (file.exists) {
        val bytes = file.readBytesSync()
        val sum = calculated.withIndex().sumOf { (index, value) -> sq(value - bytes[index]) }
        logger.info("Error: $sum, ${sum.toDouble() / calculated.size}")
    } else {
        file.writeBytes(calculated)
    }

    Engine.requestShutdown()
}