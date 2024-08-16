package me.anno.image.jpg

import me.anno.image.ImageTransform
import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback
import me.anno.utils.types.Buffers.skip
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadataNode

object ExifOrientation {

    fun findRotation(src: FileReference, callback: Callback<ImageTransform?>) {
        src.inputStream { input0, err ->
            if (input0 != null) {
                val result = getExifResultFromStream(input0) { bytes ->
                    if (isExifStart(bytes)) {
                        findOrientation(bytes)
                    } else null
                }
                callback.ok(result)
            } else callback.err(err)
        }
    }

    private val orientations = listOf(
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 0),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 180),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = true, 0),
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 270),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 90),
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 90),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 270)
    )

    private fun <V> getExifResultFromStream(input0: InputStream, getResult: (ByteArray) -> V?): V? {
        val input = ImageIO.createImageInputStream(input0)
        for (reader in ImageIO.getImageReaders(input)) {
            try {
                reader.input = input
                val result = getExifResultFromImageReader(reader, getResult)
                if (result != null) return result
            } catch (_: Exception) {
            } finally {
                reader.dispose()
            }
        }
        return null
    }

    private fun <V> getExifResultFromImageReader(reader: ImageReader, getResult: (ByteArray) -> V?): V? {
        // todo we probably could implement this ourselves to get rid of the javax dependency
        val metadata = reader.getImageMetadata(0) ?: return null
        val rootNode = metadata.getAsTree("javax_imageio_jpeg_image_1.0")
        val childNodes = rootNode.childNodes
        // Look for the APP1 containing Exif data, and retrieve it.
        for (i in 0 until childNodes.length) {
            val item = childNodes.item(i)
            if (item.nodeName == "markerSequence") {
                val markerSequenceChildren = item.childNodes
                for (j in 0 until markerSequenceChildren.length) {
                    val metadataNode = markerSequenceChildren.item(j) as? IIOMetadataNode ?: continue
                    val bytes = metadataNode.userObject as? ByteArray ?: continue
                    val result = getResult(bytes)
                    if (result != null) return result
                }
            }
        }
        return null
    }

    private fun findOrientation(bytes: ByteArray): ImageTransform? {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.skip(14)
        buffer.order(getByteOrder(bytes))
        val fields = buffer.short.toInt()
        for (k in 0 until fields) {
            buffer.position(16 + k * 12)
            val tag = buffer.short.toInt()
            if (tag == 0x0112) {
                buffer.skip(6)
                val code = buffer.short.toInt()
                return orientations.getOrNull(code - 2)
            }
        }
        return null
    }

    private fun isExifStart(bytes: ByteArray): Boolean {
        return bytes.size > 16 + 12 &&
                bytes[0].toInt() == 'E'.code &&
                bytes[1].toInt() == 'x'.code &&
                bytes[2].toInt() == 'i'.code &&
                bytes[3].toInt() == 'f'.code &&
                bytes[4].toInt() == 0 &&
                bytes[6] == bytes[7] && // II/MM
                (bytes[6].toInt() == 'I'.code || bytes[6].toInt() == 'M'.code)
    }

    private fun getByteOrder(bytes: ByteArray): ByteOrder {
        return if (bytes[6].toInt() == 'I'.code) ByteOrder.LITTLE_ENDIAN
        else ByteOrder.BIG_ENDIAN
    }
}