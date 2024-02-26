package me.anno.jvm.images

import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

object MetadataImpl {
    fun readImageIOMetadata(file: FileReference, signature: String?, dst: MediaMetadata): Boolean {
        return when (signature) {
            "png", "jpg", "psd", "exr", "webp" -> {
                // webp supports video, but if so, FFMPEG doesn't seem to support it -> whatever, use ImageIO :)
                for (reader in ImageIO.getImageReadersBySuffix(signature)) {
                    try {
                        file.inputStreamSync().use { input: InputStream ->
                            reader.input = ImageIO.createImageInputStream(input)
                            dst.setImage(reader.getWidth(reader.minIndex), reader.getHeight(reader.minIndex))
                        }
                        break
                    } catch (_: IOException) {
                    } finally {
                        reader.dispose()
                    }
                }
                true
            }
            else -> false
        }
    }
}