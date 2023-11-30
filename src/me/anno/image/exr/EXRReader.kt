package me.anno.image.exr

import me.anno.image.raw.CompositeFloatBufferImage
import me.anno.image.raw.IFloatImage
import me.anno.utils.OS.desktop
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.float16ToFloat32
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.Version
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.EXRHeader
import org.lwjgl.util.tinyexr.EXRImage
import org.lwjgl.util.tinyexr.EXRVersion
import org.lwjgl.util.tinyexr.TinyEXR.*
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Uses TinyEXR to read EXR files; only some are supported, so try every single file before shipping!
 * */
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
        nFreeEXRErrorMessage(err[0])
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

    fun read(input: InputStream): IFloatImage {
        val bytes = input.readBytes()
        val buffer = ByteBufferPool.allocateDirect(bytes.size)
        buffer.put(bytes).flip()
        return read(buffer)
    }

    fun read(memory: ByteBuffer): IFloatImage {

        val version = EXRVersion.create()
        val err = BufferUtils.createPointerBuffer(1)

        check(ParseEXRVersionFromMemory(version, memory)) {
            version.free()
        }

        LOGGER.debug("v${version.version()}, multipart? ${version.multipart()}, tiled? ${version.tiled()}")

        if (version.multipart()) {

            throw IOException("Multipart EXR not supported")

            // these are images with multiple layers/images within one file

            val headersP = PointerBuffer.allocateDirect(1)
            val numHeadersU = ByteBufferPool.allocateDirect(4)
            val numHeadersI = numHeadersU.asIntBuffer()
            check(ParseEXRMultipartHeaderFromMemory(headersP, numHeadersI, version, memory, err), err)

            println("Version: ${Version.getVersion()}")

            val numHeaders = numHeadersI[0]
            LOGGER.debug("#Headers: $numHeaders")

            println("Header@${headersP[0]}")
            val headersP1 = MemoryUtil.memPointerBuffer(headersP[0], numHeaders)
            for (i in 0 until numHeaders) {
                val header = EXRHeader.create(headersP1[i])
                val numChannels = header.num_channels()
                println("Header[$i]: $numChannels channels, ${header.chunk_count()} chunks")
                // not working :/
                // val image = readImage(version, header, memory, err)
                // println("Image[$i]: $image")
                for (j in 0 until numChannels) {
                    val channel = header.channels()[j]
                    println("Channel[$i][$j]: ${channel.nameString()}, type ${channel.pixel_type()}, xs ${channel.x_sampling()}, ys ${channel.y_sampling()}")
                }
            }

            val imagesU = ByteBufferPool.allocateDirect(numHeaders * EXRImage.SIZEOF)
            val imagesI = EXRImage.Buffer(imagesU)
            for (i in 0 until numHeaders) {
                InitEXRImage(imagesI[i])
            }

            // "this is crashing, I am doing sth wrong, but don't know how to do it correctly"

            println("// parsed header 1st time, $imagesU")

            check(LoadEXRMultipartImageFromMemory(imagesI, headersP1, memory, err), err)

            println("// loaded exr from memory, $imagesI")

            for (i in 0 until numHeaders) {
                // todo this sometimes crashes with segfaults... why??
                val header = EXRHeader.create(headersP1[i])
                val image = readImage(header, imagesI[i])
                println("-> $image")
                header.free()
                image.write(desktop.getChild("exr/sub$i.png"))
            }

            // we can try to free stuff here...
            headersP.free()
            ByteBufferPool.free(numHeadersU)
            version.free()
            err.free()

            // return images

            TODO()

        } else {
            val header = EXRHeader.create()
            // InitEXRHeader(header)
            check(ParseEXRHeaderFromMemory(header, version, memory, err)) {
                LOGGER.debug("Error, v${version.version()}, multi? ${version.multipart()}, tiled? ${version.tiled()}")
                header.free()
                version.free()
            }
            return readImage(version, header, memory, err)
        }
    }

    private fun readImage(version: EXRVersion, header: EXRHeader, memory: ByteBuffer, err: PointerBuffer): IFloatImage {

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

        return readImage(header, image)

    }

    private fun readImage(
        header: EXRHeader,
        image: EXRImage
    ): IFloatImage {

        val width = image.width()
        val height = image.height()
        val numPixels = width * height
        val numChannels = image.num_channels()

        val channels = header.channels()
        val channelNames = Array(numChannels) {
            channels[it].nameString().lowercase()
        }

        val pixelTypes = IntArray(numChannels) {
            // 0: u32, 1: half, 2: float
            // channels2[it].p_linear(): linear vs sRGB
            channels[it].pixel_type()
        }

        LOGGER.debug(
            "$width x $height x $numChannels, ${image.num_tiles()} tiles, " +
                    "channels: ${channelNames.joinToString()}, types: ${pixelTypes.joinToString()}"
        )

        val images = image.images()!!
        var floats = Array(numChannels) { channelIndex ->
            LOGGER.debug("Image[$channelIndex, ${channelNames[channelIndex]}]: @${images[channelIndex]}")
            val dst = ByteBuffer.allocateDirect(numPixels * 4).asFloatBuffer()
            val channel = channels[channelIndex]
            when (channel.pixel_type()) {
                -2 -> { // 0: u32, correct??? looks like it in ComputeChannelLayout
                    val src = images.getByteBuffer(channelIndex, 4 * numPixels)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer()
                    for (i in 0 until numPixels) {
                        dst.put(i, src[i].toFloat())
                    }
                }
                -1 -> { // 1: half; theoretically used, but the library returns float
                    val src = images.getByteBuffer(2 * numPixels)
                        .order(ByteOrder.nativeOrder())
                        .asShortBuffer()
                    for (i in 0 until numPixels) {
                        dst.put(i, float16ToFloat32(src[i].toInt()))
                    }
                }
                0, 1, 2 -> { // 2: float
                    val src = images.getFloatBuffer(channelIndex, numPixels)
                    dst.put(src).flip()
                }
                else -> throw NotImplementedError()
            }
            dst
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

        return CompositeFloatBufferImage(width, height, floats)

    }

}