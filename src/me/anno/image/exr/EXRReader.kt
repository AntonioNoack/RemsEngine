package me.anno.image.exr

import me.anno.image.Image
import me.anno.image.raw.FloatBufferImage2
import me.anno.io.files.FileReference
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.EXRHeader
import org.lwjgl.util.tinyexr.EXRImage
import org.lwjgl.util.tinyexr.EXRVersion
import org.lwjgl.util.tinyexr.TinyEXR.*
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

object EXRReader {

    private val LOGGER = LogManager.getLogger(EXRReader::class)

    // info about the format:
    // https://openexr.readthedocs.io/en/latest/OpenEXRFileLayout.html

    private fun check(ret: Int, cleanup: () -> Unit = {}) {
        if (ret == TINYEXR_SUCCESS) return
        cleanup()
        when (ret) {
            TINYEXR_ERROR_INVALID_MAGIC_NUMBER -> throw IOException("Invalid magic")
            TINYEXR_ERROR_INVALID_EXR_VERSION -> throw IOException("Invalid version")
            TINYEXR_ERROR_INVALID_ARGUMENT -> throw IOException("Invalid argument")
            TINYEXR_ERROR_INVALID_DATA -> throw IOException("Invalid data")
            TINYEXR_ERROR_INVALID_PARAMETER -> throw IOException("Invalid parameter")
            TINYEXR_ERROR_UNSUPPORTED_FORMAT -> throw IOException("Unsupported format")
            TINYEXR_ERROR_INVALID_HEADER -> throw IOException("Invalid header")
            TINYEXR_ERROR_UNSUPPORTED_FEATURE -> throw IOException("Unsupported feature")
            else -> throw IOException("Invalid EXR File, #$ret")
        }
    }

    private fun check(ret: Int, err: PointerBuffer, cleanup: () -> Unit = {}) {
        if (ret == TINYEXR_SUCCESS) return
        LOGGER.debug(MemoryUtil.memASCIISafe(err[0]) ?: "null")
        cleanup()
        when (ret) {
            TINYEXR_ERROR_INVALID_MAGIC_NUMBER -> throw IOException("Invalid magic")
            TINYEXR_ERROR_INVALID_EXR_VERSION -> throw IOException("Invalid version")
            TINYEXR_ERROR_INVALID_ARGUMENT -> throw IOException("Invalid argument")
            TINYEXR_ERROR_INVALID_DATA -> throw IOException("Invalid data")
            TINYEXR_ERROR_INVALID_PARAMETER -> throw IOException("Invalid parameter")
            TINYEXR_ERROR_UNSUPPORTED_FORMAT -> throw IOException("Unsupported format")
            TINYEXR_ERROR_INVALID_HEADER -> throw IOException("Invalid header")
            TINYEXR_ERROR_UNSUPPORTED_FEATURE -> throw IOException("Unsupported feature")
            else -> throw IOException("Invalid EXR File, #$ret")
        }
    }

    private inline fun <reified V> mapChannels(channels: Array<V>, src: Array<String>, dst: String): Array<V> {
        return Array(channels.size) { dstIndex ->
            val dstName = dst[dstIndex]
            channels[src.indexOfFirst { srcName -> srcName[0] == dstName }]
        }
    }

    fun read(input: InputStream): Image {
        val bytes = input.readBytes()
        val buffer = ByteBufferPool.allocateDirect(bytes.size)
        buffer.put(bytes).flip()
        return read(buffer)
    }

    fun read(memory: ByteBuffer): Image {

        val version = EXRVersion.create()
        check(ParseEXRVersionFromMemory(version, memory)) {
            version.free()
        }

        val err = BufferUtils.createPointerBuffer(1)
        if (version.multipart()) {

            throw IOException("Multipart EXR not supported")

            // these are images with multiple layers/images within one file

            val headers = PointerBuffer.allocateDirect(1)
            val numHeaders0 = ByteBufferPool.allocateDirect(4)
            val numHeaders1 = numHeaders0.asIntBuffer()
            check(ParseEXRMultipartHeaderFromMemory(headers, numHeaders1, version, memory, err), err)

            val numHeaders2 = numHeaders1[0]

            val headers2 = EXRHeader.Buffer(headers[0], numHeaders2)
            val images0 = ByteBufferPool.allocateDirect(numHeaders2 * EXRImage.SIZEOF)
            val images1 = EXRImage.Buffer(images0)

            val headers3 = PointerBuffer.allocateDirect(numHeaders2)
            for (i in 0 until numHeaders2) {
                headers3.put(i, headers2.address())
            }

            // "this is crashing, I am doing sth wrong, but don't know how to do it correctly"

            check(LoadEXRMultipartImageFromMemory(images1, headers3, memory, err), err)

            val images2 = images1.images()!!

            // todo support both types
            // 4. Access image data
            // `exr_image.images` will be filled when EXR is scanline format.
            // `exr_image.tiled` will be filled when EXR is tiled format.

            val images = Array(numHeaders2) {

                val image = EXRImage.create(images2[it])
                // InitEXRImage(image)
                val header = EXRHeader.create(headers3[it])
                readImage(version, header, image)
            }

            // we can try to free stuff here...
            headers.free()
            ByteBufferPool.free(numHeaders0)
            version.free()
            err.free()

            // return images

        }

        val header = EXRHeader.create()
        InitEXRHeader(header)
        check(ParseEXRHeaderFromMemory(header, version, memory, err)) {
            header.free()
            version.free()
        }

        return readImage(version, header, memory, err)

    }

    private fun readImage(version: EXRVersion, header: EXRHeader, memory: ByteBuffer, err: PointerBuffer): Image {

        for (i in 0 until header.num_channels()) {
            if (header.pixel_types()[i] == TINYEXR_PIXELTYPE_HALF) {
                header.requested_pixel_types().put(i, TINYEXR_PIXELTYPE_FLOAT)
            }
        }

        val image = EXRImage.create()
        InitEXRImage(image)

        check(LoadEXRImageFromMemory(image, header, memory, err)) {
            header.free()
            version.free()
            image.free()
        }

        return readImage(version, header, image)

    }

    private fun readImage(
        version: EXRVersion,
        header: EXRHeader?,
        image: EXRImage
    ): Image {

        val width = image.width()
        val height = image.height()
        val numChannels = image.num_channels()

        val channels2 = header!!.channels()
        val channelNames = Array(numChannels) {
            channels2[it].nameString().lowercase()
        }

        LOGGER.debug("$width x $height x $numChannels, ${image.num_tiles()}")

        val images = image.images()!!
        var floats = Array(numChannels) { channelIndex ->
            images.getFloatBuffer(channelIndex, width * height)
        } // BGR it seems

        // these sometimes crashed it...
        // image.free()
        // header.free()
        // version.free()

        if (numChannels == 3 && "r" in channelNames && "g" in channelNames && "b" in channelNames) {
            floats = mapChannels(floats, channelNames, "rgb")
        }

        if (numChannels == 4 && "r" in channelNames && "g" in channelNames && "b" in channelNames && "a" in channelNames) {
            floats = mapChannels(floats, channelNames, "rgba")
        }

        return FloatBufferImage2(width, height, floats)

    }

}