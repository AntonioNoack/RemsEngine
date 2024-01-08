package me.anno.image.gimp

import me.anno.image.Image
import me.anno.image.ImageReader
import me.anno.image.raw.ByteImage
import me.anno.image.raw.FloatImage
import me.anno.image.raw.IntImage
import me.anno.io.Streams.readBE32
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Buffers.skip
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Gimp is a painting program, we regularly use,
 * and reading its files would be nice, so here is
 * the official Gimp implementation ported from C to Java.
 * If only supports layer with the format U8-linear
 * [Gimp Repository](https://github.com/GNOME/gimp/)
 * */
class GimpImage {

    companion object {

        private val LOGGER = LogManager.getLogger(GimpImage::class)

        private const val TILE_SIZE = 64
        const val MAGIC = "gimp xcf "

        fun findSize(data: InputStream): IntPair {
            for (char in MAGIC) {
                if (data.read() != char.code)
                    throw IOException("Magic doesn't match")
            }
            // could be made more efficient, but probably doesn't matter
            val fileThing = ByteArray(5) { data.read().toByte() }.decodeToString()
            if (!fileThing.startsWith("textures/fileExplorer") && !(fileThing[0] == 'v' && fileThing[4] == 0.toChar())) {
                throw IOException("Expected 'file' or 'v'-version")
            }
            val width = data.readBE32()
            val height = data.readBE32()
            if (width <= 0 || height <= 0) throw IOException("Image must not be empty $width x $height")
            return IntPair(width, height)
        }

        fun readImage(file: FileReference, callback: (GimpImage?, Exception?) -> Unit) {
            file.readByteBuffer(false) { data, exc ->
                if (data != null) {
                    data.order(ByteOrder.BIG_ENDIAN)
                    callback(readImage(data), null)
                } else callback(null, exc)
            }
        }

        fun readImage(input: InputStream): GimpImage {
            val bytes = input.readBytes()
            input.close()
            val data = ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN)
            return readImage(data)
        }

        fun read(file: FileReference, callback: (Image?, Exception?) -> Unit) {
            readImage(file) { it, exc ->
                if (it != null) callback(combineLayers(it), null)
                else callback(null, exc)
            }
        }

        fun read(input: InputStream): Image {
            return combineLayers(readImage(input))
        }

        fun combineLayers(gimpImage: GimpImage): Image {
            val hasAlpha = gimpImage.layers.any {
                val image = it.image
                image != null && (image.hasAlphaChannel || it.opacity < 255)
            }
            val w = gimpImage.width
            val h = gimpImage.height
            // todo if the source has HDR contents, use HDR here as well
            val dst = IntArray(w * h)
            for (layer in gimpImage.layers) {
                val image = layer.image
                if (layer.isVisible && image != null && layer.opacity > 0) {
                    val dx = layer.x
                    val dy = layer.y
                    val x0 = max(dx, 0)
                    val y0 = max(dy, 0)
                    val x1 = min(dx + layer.width, w)
                    val y1 = min(dy + layer.height, h)
                    if (x1 <= x0 || y1 <= x0) continue
                    val opacity = layer.opacity
                    if (image.hasAlphaChannel || opacity < 255) {
                        for (y in y0 until y1) {
                            var dstIndex = x0 + y * w
                            var srcIndex = (x0 - dx) + (y - dy) * layer.width
                            // layer.blendSpace
                            for (x in x0 until x1) {
                                // todo theoretically, we would need to apply the correct blending function here
                                val color = image.getRGB(srcIndex++)
                                dst[dstIndex] = blend(dst[dstIndex], color, opacity)
                                dstIndex++
                            }
                        }
                    } else {
                        for (y in y0 until y1) {
                            var dstIndex = x0 + y * w
                            var srcIndex = (x0 - dx) + (y - dy) * layer.width
                            for (x in x0 until x1) {
                                dst[dstIndex++] = image.getRGB(srcIndex++)
                            }
                        }
                    }
                }
            }
            val reallyHasAlpha = hasAlpha && dst.any { it.ushr(24) != 255 }
            return IntImage(w, h, dst, reallyHasAlpha)
        }

        private fun readImage(data: ByteBuffer): GimpImage {
            for (char in MAGIC) {
                if (data.get() != char.code.toByte())
                    throw IOException("Magic doesn't match")
            }
            val image = GimpImage()
            image.readContent(data)
            return image
        }

        private fun blend(dst: Int, src: Int, opacity: Int): Int {
            val aa = dst.a()
            val iaa = 255 - aa
            val ba = (src.a() * opacity * iaa) / (255 * 255) // ^1
            val alpha = aa + ba // ^1
            if (alpha == 0) return 0 // prevent division by zero
            val nr = (dst.r() * aa + src.r() * ba) / alpha
            val ng = (dst.g() * aa + src.g() * ba) / alpha
            val nb = (dst.b() * aa + src.b() * ba) / alpha
            return rgba(nr, ng, nb, alpha)
        }

        fun readAsFolder(file: FileReference, callback: (InnerFolder?, Exception?) -> Unit) {
            file.inputStream { it, exc ->
                if (it != null) {
                    readAsFolder(file, it, callback)
                } else callback(null, exc)
            }
        }

        fun readAsFolder(file: FileReference, input: InputStream, callback: (InnerFolder?, Exception?) -> Unit) {
            val info = readImage(input)
            ImageReader.readAsFolder(file) { folder, exc ->
                if (folder != null) {
                    val subFolder = folder.createChild("layers", "layers")
                    val layers = info.layers
                    for (i in layers.indices) {
                        val layer = layers[i]
                        val image = layer.image
                        if (image != null) {
                            subFolder.createImageChild(layer.name + ".png", image)
                        }
                    }
                    callback(folder, null)
                } else callback(null, exc)
            }
        }
    }

    var fileVersion = 0
    var bytesPerOffset = 4
    var compression = Compression.NONE
    var precision = DataType.U8_NON_LINEAR

    var width = 0
    var height = 0
    var imageType = ImageType.INDEXED

    var propType = PropertyType.END
    var propSize = 0

    var colorMap: IntArray? = null

    var tmp: ByteArray? = null

    var channels = ArrayList<Channel>()
    var layers = ArrayList<Layer>()

    fun readContent(data: ByteBuffer) {

        val fileThing = ByteArray(5) { data.get() }.decodeToString()
        fileVersion = if (fileThing.startsWith("textures/fileExplorer")) {
            0
        } else if (fileThing[0] == 'v' && fileThing[4] == 0.toChar()) {
            fileThing.substring(1, 4).toInt()
        } else throw IOException("Expected 'file' or 'v'-version")

        if (fileVersion >= 11) {
            bytesPerOffset = 8
        }

        width = data.int
        height = data.int
        if (width <= 0 || height <= 0) throw IOException("Image must not be empty $width x $height")

        imageType = ImageType.entries.getOrNull(data.int) ?: throw IOException("Unknown image type")

        precision = if (fileVersion >= 4) {
            val p = data.int
            when (fileVersion) {
                4 -> when (p) {
                    0 -> DataType.U8_NON_LINEAR
                    1 -> DataType.U16_NON_LINEAR
                    2 -> DataType.U32_LINEAR
                    3 -> DataType.HALF_LINEAR
                    4 -> DataType.FLOAT_LINEAR
                    else -> throw IOException("Unknown precision")
                }
                5, 6 -> {
                    when (p) {
                        100 -> DataType.U8_LINEAR
                        150 -> DataType.U8_NON_LINEAR
                        200 -> DataType.U16_LINEAR
                        250 -> DataType.U16_NON_LINEAR
                        300 -> DataType.U32_LINEAR
                        350 -> DataType.U32_NON_LINEAR
                        400 -> DataType.HALF_LINEAR
                        450 -> DataType.HALF_NON_LINEAR
                        500 -> DataType.FLOAT_LINEAR
                        550 -> DataType.FLOAT_NON_LINEAR
                        else -> throw IOException("Unknown precision")
                    }
                }
                else -> DataType.entries.firstOrNull { it.value == p }
                    ?: throw IOException("Unknown precision")
            }
        } else DataType.U8_NON_LINEAR

        // just creates a new, empty instance
        // val image = createImage(width, height, imageType, precision, false)

        loadImageProps(data)

        // read layers
        while (true) {
            val offset = readOffset(data)
            if (offset == 0) break // end of list
            val savedPosition = data.position()
            data.position(offset)
            val layer = loadLayer(data)
            if (layer != null) layers.add(layer)
            data.position(savedPosition)
        }

        // read channels
        while (true) {
            val offset = readOffset(data)
            if (offset == 0) break
            val savedPosition = data.position()
            data.position(offset)
            val channel = loadChannel(data)
            // add channel to image
            if (channel != null) channels.add(channel)
            data.position(savedPosition)
        }

        // loadAddMasks()?

        // pretty much done
    }

    fun createImage(width: Int, height: Int, imageType: ImageType, format: DataType, hasAlpha: Boolean): Image {
        val channels = imageType.channels + hasAlpha.toInt(1)
        return when (format) {
            DataType.U8_LINEAR,
            DataType.U8_NON_LINEAR,
            DataType.U8_PERCEPTUAL -> ByteImage(
                width, height,
                when (channels) {
                    1 -> ByteImage.Format.R
                    2 -> ByteImage.Format.RG
                    3 -> ByteImage.Format.RGB
                    4 -> ByteImage.Format.ARGB
                    else -> throw IOException("Too many channels in image")
                }
            )
            else -> {
                LOGGER.warn("Got format $format, just using float image")
                FloatImage(width, height, 1)
            }
        }
    }

    private fun readString(data: ByteBuffer): String {
        val size = data.int - 1
        if (size == 0) return ""
        val str = ByteArray(size) { data.get() }.decodeToString()
        data.get() // \0
        return str
    }

    private fun loadLayer(data: ByteBuffer): Layer? {

        var width = data.int
        var height = data.int
        val type = data.int
        val name = readString(data)

        val hasAlpha = type.and(1) != 0
        val baseType = when (type) {
            0, 1 -> ImageType.RGB
            2, 3 -> ImageType.GRAY
            4, 5 -> ImageType.INDEXED
            else -> throw IOException()
        }

        val layer = Layer(width, height, name, baseType, hasAlpha)

        if (width <= 0 || height <= 0) {
            var isGroupLayer = false
            var isTextLayer = false
            val savedPosition = data.position()
            LOGGER.warn("check layer props")
            if (isTextLayer || isGroupLayer) {
                data.position(savedPosition)
                width = 1
                height = 1
            } else return null
        }

        // create a new layer theoretically
        loadLayerProps(data, layer)

        val hierarchyOffset = readOffset(data)
        // val layerMaskOffset = readOffset(info, data)

        // if sth
        data.position(hierarchyOffset)
        layer.image = loadBuffer(data, layer)

        // and stuff...

        return layer
    }

    private fun loadBuffer(data: ByteBuffer, layer: Layer): Image? {
        /*val width = data.int
        val height = data.int
        val bpp = data.int*/
        data.skip(12)
        val offset = readOffset(data)
        data.position(offset)
        val format = DataType.U8_LINEAR // idk...
        return loadLevel(data, format, layer)
    }

    private fun loadBuffer(data: ByteBuffer, channel: Channel) {
        /*val width = data.int
        val height = data.int
        val bpp = data.int*/
        data.skip(12)
        val offset = readOffset(data)
        data.position(offset)
        loadLevel(data, channel)
    }

    private fun loadLevel(data: ByteBuffer, dataType: DataType, layer: Layer): Image? {
        val bpp = getBpp(dataType, layer.baseType, layer.hasAlpha)
        val width = data.int
        val height = data.int
        val image = createImage(width, height, layer.baseType, dataType, layer.hasAlpha)
        // first tile offset
        var offset = readOffset(data)
        if (offset == 0) return null // empty
        val tileXs = ceilDiv(width, TILE_SIZE)
        val tileYs = ceilDiv(height, TILE_SIZE)
        val tiles = tileXs * tileYs
        val maxDataLength = bpp * TILE_SIZE * TILE_SIZE * 3 / 2
        if (tmp == null || tmp!!.size < maxDataLength) {
            tmp = ByteArray(maxDataLength)
        }
        for (tileIndex in 0 until tiles) {
            if (offset == 0) throw IOException("Not enough tiles found")
            val savedPosition = data.position()
            var offset2 = readOffset(data)
            // "if the offset is 0 then we need to read in the maximum possible
            // allowing for negative compression"
            if (offset2 == 0) offset2 = offset + maxDataLength
            if (offset2 < offset || offset2 - offset > maxDataLength) {
                LOGGER.warn("Invalid tile data length in tile $tileIndex/$tiles: $offset2 < $offset || $offset2 - $offset (${offset2 - offset}) > $maxDataLength")
                return image
            }
            data.position(offset)
            // get rect...
            val x0 = (tileIndex % tileXs) * TILE_SIZE
            val y0 = (tileIndex / tileXs) * TILE_SIZE
            val dataLength = offset2 - offset
            when (compression) {
                Compression.NONE -> loadTile(data, image, layer.baseType, dataType, x0, y0)
                Compression.RLE -> loadTileRLE(data, image, layer.baseType, dataType, x0, y0, dataLength)
                else -> throw IOException("Compression not supported")
            }
            data.position(savedPosition)
            offset = readOffset(data)
        }
        if (offset != 0) throw IOException("Encountered garbage after reading level")
        return image
    }

    private fun readComponents(
        bppDivComponents: Int,
        tileData: ByteArray,
        tileSizeDivBppXComponents: Int
    ) {
        LOGGER.warn("todo: readComponents")
    }

    private fun readFromBE(
        bppDivComponents: Int,
        tileData: ByteArray,
        tileSizeDivBppXComponents: Int
    ) {
        LOGGER.warn("todo: readFromBE")
    }

    private fun getBpp(dataType: DataType, imageType: ImageType, hasAlpha: Boolean): Int {
        return dataType.bpp * (imageType.channels + hasAlpha.toInt(1))
    }

    private fun loadTile(
        data: ByteBuffer, image: Image,
        imageType: ImageType, dataType: DataType, x0: Int, y0: Int
    ) {

        val bpp = getBpp(dataType, imageType, image.hasAlphaChannel)
        val tileWidth = min(TILE_SIZE, image.width - x0)
        val tileHeight = min(TILE_SIZE, image.height - y0)
        val tileSize = bpp * tileWidth * tileHeight

        // just read the data
        // todo we probably need to convert the format from rgba to argb
        LOGGER.warn("Colors probably need to be converted from rgba to argb")
        val tileData = tmp!!
        for (i in 0 until tileSize) {
            tileData[i] = data.get()
        }

        if (fileVersion >= 12) {
            readComponents(dataType.bpp, tileData, tileWidth * tileHeight)
        }

        fillTileIntoImage(dataType, image, x0, y0, tileWidth, tileHeight, bpp, tileData)
    }

    private fun mapChannel(c: Int, hasAlpha: Boolean): Int {
        // rgba -> argb
        return if (hasAlpha) (c + 1).and(3) else c
    }

    private fun loadTileRLE(
        data: ByteBuffer, image: Image,
        imageType: ImageType, format: DataType, x0: Int, y0: Int, dataLength: Int
    ) {
        val bpp = getBpp(format, imageType, image.hasAlphaChannel)
        val tileWidth = min(TILE_SIZE, image.width - x0)
        val tileHeight = min(TILE_SIZE, image.height - y0)
        if (dataLength <= 0) return

        val tileData = tmp!!
        try {
            for (i in 0 until bpp) {
                var size = tileWidth * tileHeight
                var count = 0
                var k = mapChannel(i, image.hasAlphaChannel)
                while (size > 0) {
                    var length = data.get().toInt() and 255
                    if (length >= 128) {
                        length = 256 - length
                        if (length == 128) length = data.short.toInt() and 0xffff
                        count += length
                        size -= length
                        while (length-- > 0) {
                            tileData[k] = data.get()
                            k += bpp
                        }
                    } else {
                        length++
                        if (length == 128) length = data.short.toInt() and 0xffff
                        count += length
                        size -= length
                        val value = data.get()
                        for (j in 0 until length) {
                            tileData[k] = value
                            k += bpp
                        }
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            LOGGER.warn("", e)
        } catch (e: BufferUnderflowException) {
            LOGGER.warn("", e)
        }

        if (fileVersion >= 12) {
            readFromBE(format.bpp, tileData, tileWidth * tileHeight)
        }

        fillTileIntoImage(format, image, x0, y0, tileWidth, tileHeight, bpp, tileData)
    }

    private fun fillTileIntoImage(
        format: DataType, image: Image,
        x0: Int, y0: Int, dx: Int, dy: Int,
        bpp: Int, tileData: ByteArray
    ) {
        when (format) {
            DataType.U8_LINEAR -> {
                image as ByteImage
                val dst = image.data
                var readIndex = 0
                for (y in y0 until y0 + dy) {
                    var writeIndex = (x0 + y * image.width) * bpp
                    for (x in 0 until dx * bpp) {
                        dst[writeIndex++] = tileData[readIndex++]
                    }
                }
            }
            else -> TODO("fill the decoded data into the image, format $format")
        }
    }

    private fun loadLevel(data: ByteBuffer, channel: Channel) {
        LOGGER.warn("todo: loadLevel")
    }

    private fun loadChannel(data: ByteBuffer): Channel? {

        val width = data.int
        val height = data.int
        if (width <= 0 || height <= 0) return null

        val name = readString(data)

        val channel = Channel(width, height, name)
        loadChannelProps(data, channel)

        val hierarchyOffset = readOffset(data)
        data.position(hierarchyOffset)

        loadBuffer(data, channel)

        return channel
    }

    private fun readOffset(data: ByteBuffer): Int {
        return if (bytesPerOffset == 4) data.int else data.long.toInt()
    }

    private fun loadProp(data: ByteBuffer) {
        propType = PropertyType.entries[data.int]
        propSize = data.int
    }

    private fun loadLayerProps(data: ByteBuffer, layer: Layer) {
        while (true) {
            loadProp(data)
            val position = data.position()
            when (propType) {
                PropertyType.END -> break
                PropertyType.OFFSETS -> {
                    layer.x = data.int
                    layer.y = data.int
                }
                PropertyType.OPACITY -> layer.opacity = clamp(data.int, 0, 255)
                PropertyType.FLOAT_OPACITY -> layer.opacity = clamp((data.float * 255).roundToInt(), 0, 255)
                PropertyType.BLEND_SPACE -> {
                    var bs = data.int
                    if (bs < 0) {// auto
                        // mmh, complicated
                        // not completely implemented here
                        bs = -bs
                    }
                    layer.blendSpace = bs
                }
                PropertyType.VISIBLE -> layer.isVisible = data.int != 0
                else -> printUnknownProperty(data, "layer-prop")
            }
            data.position(position + propSize)
        }
    }

    private fun loadImageProps(data: ByteBuffer) {
        while (true) {
            loadProp(data)
            val position = data.position()
            when (propType) {
                PropertyType.END -> break
                PropertyType.COLOR_MAP -> {
                    val size = data.int
                    val colors = if (fileVersion == 0) {
                        data.position(data.position() + size)
                        IntArray(size) { it * 0x10101 }
                        // data is already filled :)
                    } else {
                        IntArray(size) { rgba(data.get(), data.get(), data.get(), -1) }
                    }
                    if (imageType == ImageType.INDEXED) {
                        colorMap = colors
                    }
                }
                PropertyType.COMPRESSION -> {
                    val ti = data.get().toInt()
                    compression = Compression.entries.getOrNull(ti)
                        ?: throw IOException("Unknown compression $ti")
                }
                else -> printUnknownProperty(data, "image-prop")
            }
            data.position(position + propSize)
        }
    }

    private fun loadChannelProps(data: ByteBuffer, channel: Channel) {
        while (true) {
            loadProp(data)
            val position = data.position()
            when (propType) {
                PropertyType.OPACITY -> channel.opacity = data.int / 255f
                PropertyType.FLOAT_OPACITY -> channel.opacity = data.float
                PropertyType.COLOR_TAG -> channel.colorTag = data.int
                PropertyType.COLOR -> channel.color = rgba(data.get(), data.get(), data.get(), -1)
                PropertyType.FLOAT_COLOR -> channel.color = rgba(data.float, data.float, data.float, 1f)
                else -> printUnknownProperty(data, "channel-prop")
            }
            data.position(position + propSize)
        }
    }

    private fun printUnknownProperty(data: ByteBuffer, type: String) {
        /*if (info.propSize == 4)
            LOGGER.debug("$type ${info.propType}, ${data.getInt(data.position())}/${data.getFloat(data.position())}")
        else LOGGER.debug("$type ${info.propType}, size ${info.propSize}")*/
    }
}