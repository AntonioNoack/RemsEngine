package me.anno.image.jpg

import me.anno.image.ImageTransform
import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback
import me.anno.utils.types.Buffers.skip
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadataNode

object ExifOrientation {

    fun findRotation(src: FileReference, callback: Callback<ImageTransform?>) {
        src.inputStream { input0, err ->
            if (input0 != null) {
                val input = ImageIO.createImageInputStream(input0)
                var rot: ImageTransform? = null
                for (reader in ImageIO.getImageReaders(input)) {
                    try {
                        reader.input = input
                        rot = getExifOrientation(reader, 0)
                        if (rot != null) break
                    } catch (_: Exception) {
                    } finally {
                        reader.dispose()
                    }
                }
                callback.ok(rot)
            } else callback.err(err)
        }
    }

    val orientations = listOf(
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 0),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 180),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = true, 0),
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 270),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 90),
        ImageTransform(mirrorHorizontal = true, mirrorVertical = false, 90),
        ImageTransform(mirrorHorizontal = false, mirrorVertical = false, 270)
    )

    fun getExifOrientation(reader: ImageReader, imageIndex: Int): ImageTransform? {
        val metadata = reader.getImageMetadata(imageIndex) ?: return null
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

                    // Exif\u00[padding \u00][II|MM for byte-order, II = Little Endian, MM = Big Endian][skip 6][*uint16 num fields],
                    // fields: each 12 bytes in size, [uint16 tag, must be 0x0112][uint16 type][uint32 count][uint16 actual value :)]
                    // int16u, group IFD0,
                    // 1 = normal, 2 = mirror x,
                    // 3 = rotate 180°, 4 = mirror y,
                    // 5 = mirror x + rotate 270° cw,
                    // 6 = rotate 90° cw, 7 = mirror x and rotate 90° cw,
                    // 8 = rotate 270° cw

                    if (bytes.size > 16 + 12 &&
                        bytes[0].toInt() == 'E'.code &&
                        bytes[1].toInt() == 'x'.code &&
                        bytes[2].toInt() == 'i'.code &&
                        bytes[3].toInt() == 'f'.code &&
                        bytes[4].toInt() == 0 &&
                        bytes[6] == bytes[7] && // II/MM
                        (bytes[6].toInt() == 'I'.code || bytes[6].toInt() == 'M'.code)
                    ) {
                        val buffer = ByteBuffer.wrap(bytes)
                        buffer.skip(14)
                        buffer.order(
                            if (bytes[6].toInt() == 'I'.code) ByteOrder.LITTLE_ENDIAN
                            else ByteOrder.BIG_ENDIAN
                        )
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
                    }
                }
            }
        }
        return null
    }
}