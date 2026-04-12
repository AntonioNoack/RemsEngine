package me.anno.image.exr

import me.anno.image.Image
import me.anno.image.raw.FloatImage
import me.anno.image.raw.HalfImage
import me.anno.io.files.FileReference
import me.anno.utils.Warning.unused
import me.anno.utils.async.Callback
import org.joml.Vector2i
import java.io.IOException

object OpenEXRReader {

    fun readImage(src: FileReference, data: ByteArray, callback: Callback<Image>) {
        unused(src)
        val result = readImage(data)
        callback.call(result as? Image, result as? Exception)
    }

    /**
     * returns FloatImage, HalfFloatImage, or Exception
     * */
    fun readImage(data: ByteArray): Any {
        val size = OpenEXRStatsReader.findSize(data.inputStream()) as? Vector2i
            ?: return IOException("Invalid EXR file")
        val channels = OpenEXRStatsReader.findChannels(data.inputStream())
        return when {
            channels.isEmpty() -> IOException("Missing channels")
            channels.any { it.pixType == EXRPixelType.FLOAT } -> {
                val image = FloatImage(size.x, size.y, channels.size)
                OpenEXRJava.readFloatImage(image, data)
            }
            else -> {
                val image = HalfImage(size.x, size.y, channels.size)
                OpenEXRJava.readHalfImage(image, data)
            }
        }
    }

}