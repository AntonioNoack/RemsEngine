package me.anno.utils.test

import me.anno.audio.AudioFXCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.objects.FourierTransform
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.roundToInt

// the audio sounds baaaad with bufferSize = 1024 .. why??

fun main() {

    val logger = LogManager.getLogger("AudioTest")

    val cache = AudioFXCache
    val fourier = FourierTransform()

    fourier.file = getReference(OS.downloads, "Aitana 11 Raizones.mp4")
    val meta = fourier.forcedMeta!!

    val keyIndices = 1..3
    val keys = keyIndices.map { fourier.getKey(it.toLong(), false) }

    logger.info(meta)

    val buffers = keys.map { cache.getBuffer0(meta, it, false)!!.timeLeft!! }

    val size = 128
    val step = 1

    val max = buffers.maxOf { v -> v.maxOf { abs(it) } }

    for (i in 1 until buffers.size) {
        val b0 = buffers[i - 1]
        val b1 = buffers[i]
        logger.info()
        logger.info("xxx skip xxx")
        logger.info()
        showValues(b0, -size, step, max)
        showValues(b1, +size, step, max)
    }


}

fun showValues(values: FloatArray, size: Int, step: Int, max: Float) {
    val logger = LogManager.getLogger("AudioTest")
    val subValues = if (size < 0) values.toList().subList(values.size + size, values.size)
    else values.toList().subList(0, size)
    logger.info("start $size")
    for (i in 0 until abs(size) step step) {
        showValue(subValues[i], max)
    }
}

fun showValue(value: Float, max: Float) {
    val logger = LogManager.getLogger("AudioTest")
    val stops = 100
    val width = 1 + stops / 2 + (value * stops * 0.5f / max).roundToInt()
    for (i in 0 until width) {
        print("=")
    }
    logger.info()
}