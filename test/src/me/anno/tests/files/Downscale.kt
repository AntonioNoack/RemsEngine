package me.anno.tests.files

import me.anno.image.ImageCPUCache
import me.anno.image.ImageScale.scaleMax
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream

fun main() {
    // an example how I downscaled lots of images for my homepage
    LogManager.disableLogger("FFMPEGMetadata")
    LogManager.disableLogger("FFMPEGStream")
    LogManager.disableLogger("BetterProcessBuilder")
    LogManager.disableLogger("ISaveable")
    LogManager.disableLogger("Clock")
    LogManager.disableLogger("FrameReader")
    val src = getReference("C:/XAMPP/htdocs/anionoa/ico512")
    val dst = getReference("C:/XAMPP/htdocs/anionoa/ico384")
    dst.mkdirs()
    val ext = listOf("png", "jpg", "webp")
    for (child in src.listChildren()!!) {
        val image = ImageCPUCache[child, false] ?: continue
        val (w, h) = scaleMax(image.width, image.height, 384)
        if (w < image.width && h < image.height) {
            val downScaled = image.resized(w, h, false)
            val data = ext.map {
                val c0 = ByteArrayOutputStream(1024)
                downScaled.write(c0, it)
                c0.flush()
                it to c0.toByteArray()
            }
            println("$child -> $image -> [$w x $h] - ${child.length()} > ${data.map { it.second.size }}")
            val best = data.minByOrNull { it.second.size }!!
            if (best.second.size < child.length()) {
                println("Saving ${(1f - best.second.size.toFloat() / child.length()).formatPercent()}%")
                if (child.lcExtension != best.first) println("            $child has been renamed to ${best.first}!")
                dst.getChild("${child.nameWithoutExtension}.${best.first}")
                    .writeBytes(best.second)
            }
        }
    }
}