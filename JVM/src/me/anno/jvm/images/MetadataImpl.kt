package me.anno.jvm.images

import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.firstPromise
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader

object MetadataImpl {

    private val supportedExtensions = "png,jpg,psd,exr,webp".split(',')

    fun readImageIOMetadata(
        file: FileReference, signature: String?,
        dst: MediaMetadata, nextReaderIndex: Int
    ): Boolean {
        if (signature !in supportedExtensions) return false
        // webp supports video, but if so, FFMPEG doesn't seem to support it -> whatever, use ImageIO :)
        firstPromise(ImageIO.getImageReadersBySuffix(signature)) { reader, cb ->
            file.inputStream(cb.map { stream ->
                tryReadImageIOMetadata(stream, reader, dst)
            })
        }.catch { dst.continueReading(nextReaderIndex) }
        return true
    }

    @Throws(IOException::class)
    private fun tryReadImageIOMetadata(stream: InputStream, reader: ImageReader, dst: MediaMetadata) {
        reader.input = ImageIO.createImageInputStream(stream)
        dst.setImageSize(reader.getWidth(reader.minIndex), reader.getHeight(reader.minIndex))
    }
}