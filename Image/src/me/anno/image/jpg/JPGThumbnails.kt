package me.anno.image.jpg

import me.anno.image.Image
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.structures.Callback
import javax.imageio.ImageIO

/**
 * Extracts embedded thumbnails in JPEGs
 *
 * The standard readers work well and good, but unfortunately,
 * then don't allow me to load the image as a small size;
 * which should definitively be possible
 * Is there a way to read JPG thumbnails from the primary data?
 * Implementing our own reader is MUCH too complicated; JPEG is insane
 * */
object JPGThumbnails {

    fun readThumbnail(file: FileReference, callback: Callback<Image?>) {
        extractThumbnail(file) { bytes ->
            if (bytes != null) {
                callback.ok(ImageIO.read(bytes.inputStream()).toImage())
            } else callback.ok(null)
        }
    }

    fun extractThumbnail(file: FileReference, callback: (ByteArray?) -> Unit) {
        // 65k is the max size for an exif section; plus 4k, where we hopefully find the marker
        val maxSize = 65536 + 4096
        file.inputStream(maxSize.toLong()) { it, _ ->
            if (it != null) {
                val data = it.readNBytes2(maxSize, false)
                callback(findData(data))
            } else callback(null)
        }
    }

    private fun findStart(data: ByteArray): Int {
        for (i in 2 until data.size - 1) {
            if (data[i] == 0xff.toByte() && data[i + 1] == 0xd8.toByte()) {
                return i
            }
        }
        return 0
    }

    private fun findData(data: ByteArray): ByteArray? {
        val start = findStart(data)
        val li = data.size - 1
        var i = start
        while (i < li) {
            if (data[i] == 0xff.toByte() && data[i + 1] == 0xd9.toByte()) {
                val end = i + 2
                return data.copyOfRange(start, end)
            }
            i++
        }
        return null
    }
}