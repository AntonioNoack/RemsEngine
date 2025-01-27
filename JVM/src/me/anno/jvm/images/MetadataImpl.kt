package me.anno.jvm.images

import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

object MetadataImpl {

    private fun isSignatureSupported(signature: String?): Boolean {
        return when (signature) {
            "png", "jpg", "psd", "exr", "webp" -> true
            else -> false
        }
    }

    fun readImageIOMetadata(file: FileReference, signature: String?, dst: MediaMetadata, ri: Int): Boolean {
        if (!isSignatureSupported(signature)) return false
        // webp supports video, but if so, FFMPEG doesn't seem to support it -> whatever, use ImageIO :)
        for (reader in ImageIO.getImageReadersBySuffix(signature)) {
            try {
                file.inputStreamSync().use { input: InputStream ->
                    reader.input = ImageIO.createImageInputStream(input)
                    dst.setImageSize(reader.getWidth(reader.minIndex), reader.getHeight(reader.minIndex))
                }
                return true
            } catch (_: IOException) {
            } finally {
                reader.dispose()
            }
        }
        return false
    }
}