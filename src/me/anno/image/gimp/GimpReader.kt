package me.anno.image.gimp

import me.anno.image.HDRImage
import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.Color.rgba
import me.anno.utils.LOGGER
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object GimpReader {

    // the internal gimp format looks quite complicated :/

    private const val GIMP_RGB = 0
    private const val GIMP_GRAY = 1
    private const val GIMP_INDEXED = 2

    private const val GIMP_RGB_IMAGE = 0
    private const val GIMP_RGBA_IMAGE = 1
    private const val GIMP_GRAY_IMAGE = 2
    private const val GIMP_GRAYA_IMAGE = 3
    private const val GIMP_INDEXED_IMAGE = 4
    private const val GIMP_INDEXEDA_IMAGE = 5

    private const val COMPRESS_NONE = 0
    private const val COMPRESS_RLE = 1
    private const val COMPRESS_ZLIB = 2 // unused, the code says
    private const val COMPRESS_FRACTAL = 3 // unused, the code says

    private const val GIMP_PRECISION_U8_LINEAR = 100
    private const val GIMP_PRECISION_U8_NON_LINEAR = 150
    private const val GIMP_PRECISION_U8_PERCEPTUAL = 175
    private const val GIMP_PRECISION_U16_LINEAR = 200
    private const val GIMP_PRECISION_U16_NON_LINEAR = 250
    private const val GIMP_PRECISION_U16_PERCEPTUAL = 275
    private const val GIMP_PRECISION_U32_LINEAR = 300
    private const val GIMP_PRECISION_U32_NON_LINEAR = 350
    private const val GIMP_PRECISION_U32_PERCEPTUAL = 375
    private const val GIMP_PRECISION_HALF_LINEAR = 500
    private const val GIMP_PRECISION_HALF_NON_LINEAR = 550
    private const val GIMP_PRECISION_HALF_PERCEPTUAL = 575
    private const val GIMP_PRECISION_FLOAT_LINEAR = 600
    private const val GIMP_PRECISION_FLOAT_NON_LINEAR = 650
    private const val GIMP_PRECISION_FLOAT_PERCEPTUAL = 675
    private const val GIMP_PRECISION_DOUBLE_LINEAR = 700
    private const val GIMP_PRECISION_DOUBLE_NON_LINEAR = 750
    private const val GIMP_PRECISION_DOUBLE_PERCEPTUAL = 775

    fun createImage(width: Int, height: Int, format: Int): Image {
        return when (format) {
            GIMP_PRECISION_U8_LINEAR,
            GIMP_PRECISION_U8_NON_LINEAR,
            GIMP_PRECISION_U8_PERCEPTUAL -> ByteImage(width, height, 1)
            else -> {
                LOGGER.warn("Got format $format, just using float image")
                HDRImage(width, height, 1)
            }
        }
    }

    fun getBBP(format: Int): Int {
        return when (format) {
            GIMP_PRECISION_U8_LINEAR,
            GIMP_PRECISION_U8_NON_LINEAR,
            GIMP_PRECISION_U8_PERCEPTUAL -> 1
            GIMP_PRECISION_U16_LINEAR,
            GIMP_PRECISION_U16_NON_LINEAR,
            GIMP_PRECISION_U16_PERCEPTUAL -> 2
            GIMP_PRECISION_U32_LINEAR,
            GIMP_PRECISION_U32_NON_LINEAR,
            GIMP_PRECISION_U32_PERCEPTUAL -> 4
            GIMP_PRECISION_HALF_LINEAR,
            GIMP_PRECISION_HALF_NON_LINEAR,
            GIMP_PRECISION_HALF_PERCEPTUAL -> 2
            GIMP_PRECISION_FLOAT_LINEAR,
            GIMP_PRECISION_FLOAT_NON_LINEAR,
            GIMP_PRECISION_FLOAT_PERCEPTUAL -> 4
            GIMP_PRECISION_DOUBLE_LINEAR,
            GIMP_PRECISION_DOUBLE_NON_LINEAR,
            GIMP_PRECISION_DOUBLE_PERCEPTUAL -> 8
            else -> throw RuntimeException("Illegal format $format")
        }
    }

    private const val PROP_END = 0
    private const val PROP_COLORMAP = 1
    private const val PROP_ACTIVE_LAYER = 2
    private const val PROP_ACTIVE_CHANNEL = 3
    private const val PROP_SELECTION = 4
    private const val PROP_FLOATING_SELECTION = 5
    private const val PROP_OPACITY = 6
    private const val PROP_MODE = 7
    private const val PROP_VISIBLE = 8
    private const val PROP_LINKED = 9
    private const val PROP_LOCK_ALPHA = 10
    private const val PROP_APPLY_MASK = 11
    private const val PROP_EDIT_MASK = 12
    private const val PROP_SHOW_MASK = 13
    private const val PROP_SHOW_MASKED = 14
    private const val PROP_OFFSETS = 15
    private const val PROP_COLOR = 16
    private const val PROP_COMPRESSION = 17
    private const val PROP_GUIDES = 18
    private const val PROP_RESOLUTION = 19
    private const val PROP_TATTOO = 20
    private const val PROP_PARASITES = 21
    private const val PROP_UNIT = 22
    private const val PROP_PATHS = 23
    private const val PROP_USER_UNIT = 24
    private const val PROP_VECTORS = 25
    private const val PROP_TEXT_LAYER_FLAGS = 26
    private const val PROP_OLD_SAMPLE_POINTS = 27
    private const val PROP_LOCK_CONTENT = 28
    private const val PROP_GROUP_ITEM = 29
    private const val PROP_ITEM_PATH = 30
    private const val PROP_GROUP_ITEM_FLAGS = 31
    private const val PROP_LOCK_POSITION = 32
    private const val PROP_FLOAT_OPACITY = 33
    private const val PROP_COLOR_TAG = 34
    private const val PROP_COMPOSITE_MODE = 35
    private const val PROP_COMPOSITE_SPACE = 36
    private const val PROP_BLEND_SPACE = 37
    private const val PROP_FLOAT_COLOR = 38
    private const val PROP_SAMPLE_POINTS = 39

    // Gimp is a program we regularly use, and reading its files would be nice :)

    class GimpInfo {

        var fileVersion = 0
        var bytesPerOffset = 4
        var compression = 0 // none
        var precision = 0

        var width = 0
        var height = 0
        var imageType = 0

        var propType = 0
        var propSize = 0

        var colorMap: IntArray? = null

        var tmp: ByteBuffer? = null

    }

    private fun readString(data: ByteBuffer, length: Int): String {
        return String(ByteArray(length) { data.get() })
    }

    private fun readString(data: ByteBuffer): String {
        val size = data.int - 1
        if (size == 0) return ""
        val str = String(ByteArray(size) { data.get() })
        data.get() // \0
        return str
    }

    private fun expectString(data: ByteBuffer, str: String) {
        for (char in str) {
            if (data.get() != char.code.toByte()) throw IOException()
        }
    }

    private fun loadStream(data: ByteBuffer) {

        expectString(data, "gimp xcf ")
        val info = GimpInfo()
        val fileThing = readString(data, 5)
        if (fileThing.startsWith("file")) {
            info.fileVersion = 0
        } else if (fileThing[0] == 'v' && fileThing[4] == 0.toChar()) {
            info.fileVersion = fileThing.substring(1, 4).toInt()
        } else throw IOException()

        if (info.fileVersion >= 11) {
            info.bytesPerOffset = 8
        }

        loadImage(info, data)

    }

    private fun loadImage(info: GimpInfo, data: ByteBuffer) {
        val width = data.int
        val height = data.int
        if (width <= 0 || height <= 0) throw IOException("Image must not be empty $width x $height")
        info.width = width
        info.height = height
        val imageType = data.int
        if (imageType !in GIMP_RGB..GIMP_INDEXED) throw IOException("Unknown image type $imageType")
        info.imageType = imageType
        info.precision = if (info.fileVersion >= 4) {
            val p = data.int
            when (info.fileVersion) {
                4 -> when (p) {
                    0 -> GIMP_PRECISION_U8_NON_LINEAR
                    1 -> GIMP_PRECISION_U16_NON_LINEAR
                    2 -> GIMP_PRECISION_U32_LINEAR
                    3 -> GIMP_PRECISION_HALF_LINEAR
                    4 -> GIMP_PRECISION_FLOAT_LINEAR
                    else -> throw IOException()
                }
                5, 6 -> {
                    when (p) {
                        100 -> GIMP_PRECISION_U8_LINEAR
                        150 -> GIMP_PRECISION_U8_NON_LINEAR
                        200 -> GIMP_PRECISION_U16_LINEAR
                        250 -> GIMP_PRECISION_U16_NON_LINEAR
                        300 -> GIMP_PRECISION_U32_LINEAR
                        350 -> GIMP_PRECISION_U32_NON_LINEAR
                        400 -> GIMP_PRECISION_HALF_LINEAR
                        450 -> GIMP_PRECISION_HALF_NON_LINEAR
                        500 -> GIMP_PRECISION_FLOAT_LINEAR
                        550 -> GIMP_PRECISION_FLOAT_NON_LINEAR
                        else -> throw IOException()
                    }
                }
                else -> p
            }
        } else GIMP_PRECISION_U8_NON_LINEAR

        // just creates a new, empty instance
        // val image = createImage(width, height, imageType, precision, false)

        loadImageProps(info, data)

        val layers = ArrayList<Layer>()

        while (true) {
            val offset = readOffset(info, data)
            if (offset == 0) break // end of list
            val savedPosition = data.position()
            data.position(offset)
            val layer = loadLayer(info, data)
            if (layer != null) layers.add(layer)
            data.position(savedPosition)
        }

        val channels = ArrayList<Channel>()

        while (true) {
            val offset = readOffset(info, data)
            if (offset == 0) break
            val savedPosition = data.position()
            data.position(offset)
            val channel = loadChannel(info, data)
            // add channel to image
            if (channel != null) channels.add(channel)
            data.position(savedPosition)
        }

        // loadAddMasks()?

        // pretty much done

        println("$width x $height, type $imageType, ${info.precision} precision, ${info.compression} compression")

    }

    class Layer(
        val width: Int, val height: Int, val name: String,
        val baseType: Int, val hasAlpha: Boolean
    ) {
        var x = 0
        var y = 0
        var opacity = 1f
        var blendSpace = 0
    }

    class Channel(
        val width: Int, val height: Int, val name: String
    ) {
        var opacity = 1f
        var color = 0
        var colorTag = 0
    }

    private fun loadLayer(info: GimpInfo, data: ByteBuffer): Layer? {

        val width = data.int
        val height = data.int
        val type = data.int
        val name = readString(data)

        var hasAlpha = false
        var baseType = GIMP_RGB

        println("loading layer $name, $width x $height, type $type")

        when (type) {
            GIMP_RGB_IMAGE -> {
            }
            GIMP_RGBA_IMAGE -> {
                hasAlpha = true
            }
            GIMP_GRAY_IMAGE -> {
                baseType = GIMP_GRAY
            }
            GIMP_GRAYA_IMAGE -> {
                baseType = GIMP_GRAY
                hasAlpha = true
            }
            GIMP_INDEXED_IMAGE -> {
                baseType = GIMP_INDEXED
            }
            GIMP_INDEXEDA_IMAGE -> {
                baseType = GIMP_INDEXED
                hasAlpha = true
            }
            else -> return null
        }

        val layer = Layer(width, height, name, baseType, hasAlpha)

        if (width <= 0 || height <= 0) {
            var isGroupLayer = false
            var isTextLayer = false
            val savedPosition = data.position()
            TODO("check layer props")
            if (isTextLayer || isGroupLayer) {
                data.position(savedPosition)
                width = 1
                height = 1
            } else return null
        }

        // create a new layer theoretically
        loadLayerProps(info, data, layer)

        val hierarchyOffset = readOffset(info, data)
        val layerMaskOffset = readOffset(info, data)

        // if sth
        data.position(hierarchyOffset)
        loadBuffer(info, data, layer)

        // and stuff...

        return layer

    }

    private fun loadLayerProps(info: GimpInfo, data: ByteBuffer, layer: Layer) {
        while (true) {
            loadProp(info, data)
            when (info.propType) {
                PROP_END -> break
                PROP_OFFSETS -> {
                    layer.x = data.int
                    layer.y = data.int
                }
                PROP_OPACITY -> layer.opacity = data.int / 255f
                PROP_FLOAT_OPACITY -> layer.opacity = data.float
                PROP_BLEND_SPACE -> {
                    var bs = data.int
                    if (bs < 0) {// auto
                        // mmh, complicated
                        // not completely implemented here
                        bs = -bs
                    }
                    layer.blendSpace = bs
                }
                PROP_COLOR_TAG -> {
                    // ??
                    skipUnknownProperty(info, data)
                }
                else -> skipUnknownProperty(info, data)
            }
        }
    }

    private fun loadBuffer(info: GimpInfo, data: ByteBuffer, channel: Layer) {
        val width = data.int
        val height = data.int
        val bpp = data.int
        val offset = readOffset(info, data)
        data.position(offset)
        val format = GIMP_PRECISION_U8_LINEAR // idk...
        val image = loadLevel(info, data, format) ?: return
        // todo print the image
        image.write(getReference(desktop, "${System.nanoTime()}.png"))
    }

    private fun loadBuffer(info: GimpInfo, data: ByteBuffer, channel: Channel) {
        val width = data.int
        val height = data.int
        val bpp = data.int
        val offset = readOffset(info, data)
        data.position(offset)
        loadLevel(info, data, channel)
    }

    private const val TILE_SIZE = 64

    private fun loadLevel(info: GimpInfo, data: ByteBuffer, format: Int): Image? {
        val bpp = getBBP(format)
        val width = data.int
        val height = data.int
        val image = createImage(width, height, format)
        // first tile offset
        var offset = readOffset(info, data)
        if (offset == 0) return null // empty
        val tileRows = (height + TILE_SIZE - 1) / TILE_SIZE
        val tileCols = (width + TILE_SIZE - 1) / TILE_SIZE
        val tiles = tileRows * tileCols
        val maxDataLength = bpp * TILE_SIZE * TILE_SIZE * 3 / 2
        info.tmp = ByteBuffer.allocate(maxDataLength)
        for (tileIndex in 0 until tiles) {
            if (offset == 0) throw IOException("Not enough tiles found")
            val savedPosition = data.position()
            var offset2 = readOffset(info, data)
            // "if the offset is 0 then we need to read in the maximum possible
            // allowing for negative compression"
            if (offset2 == 0) offset2 = offset + maxDataLength
            data.position(offset)
            if (offset2 < offset || offset2 - offset > maxDataLength) {
                LOGGER.warn("Invalid tile data length in tile $tileIndex/$tiles: $offset2 < $offset || $offset2 - $offset > $maxDataLength")
                return image
            }
            // get rect...
            val x0 = (tileIndex / tileRows) * TILE_SIZE
            val y0 = (tileIndex % tileRows) * TILE_SIZE
            val dataLength = offset2 - offset
            when (info.compression) {
                COMPRESS_NONE -> loadTile(info, data, image, format, x0, y0, dataLength)
                COMPRESS_RLE -> loadTileRLE(info, data, image, format, x0, y0, dataLength)
                else -> throw IOException("Compression not supported")
            }
            data.position(savedPosition)
            offset = readOffset(info, data)
        }
        if (offset != 0) throw IOException("Encountered garbage after reading level")
        return image
    }

    fun loadTile(info: GimpInfo, data: ByteBuffer, image: Image, format: Int, x0: Int, y0: Int, dataLength: Int) {
        TODO()
    }

    fun loadTileRLE(info: GimpInfo, data: ByteBuffer, image: Image, format: Int, x0: Int, y0: Int, dataLength: Int) {
        val bpp = getBBP(format)
        val tileWidth = min(TILE_SIZE, image.width - x0)
        val tileHeight = min(TILE_SIZE, image.height - y0)
        val tileSize = bpp * tileWidth * tileHeight
        if (dataLength <= 0) return
        val tmp = info.tmp!!
        for (i in 0 until bpp) {
            tmp.position(i)
            var size = tileWidth * tileHeight
            var count = 0
            while (size > 0) {
                var length = data.get().toInt() and 255
                if (length >= 128) {
                    length = 256 - length
                    if (length == 128) length = data.short.toInt() and 0xffff
                    count += length
                    size -= length
                    while (length-- > 0) {
                        tmp.put(data.get())
                        tmp.position(tmp.position() + bpp - 1)
                    }
                } else {
                    length++
                    if (length == 128) length = data.short.toInt() and 0xffff
                    count += length
                    size -= length
                    val value = data.get()
                    for (j in 0 until length) {
                        tmp.put(value)
                        tmp.position(tmp.position() + bpp - 1)
                    }
                }
            }
        }
        tmp.position(0)
        when (format) {
            GIMP_PRECISION_U8_LINEAR -> {
                image as ByteImage
                val dst = image.data
                var i = 0
                for (y in 0 until tileHeight) {
                    for (x in 0 until tileWidth) {
                        dst[i++] = tmp.get()
                    }
                }
            }
            else -> TODO("fill the decoded data into the image, format $format")
        }

    }

    fun loadLevel(info: GimpInfo, data: ByteBuffer, channel: Channel) {
        TODO()
    }

    private fun loadChannel(info: GimpInfo, data: ByteBuffer): Channel? {

        val width = data.int
        val height = data.int
        if (width <= 0 || height <= 0) return null

        val name = readString(data)

        println("channel $name, $width x $height")

        val channel = Channel(width, height, name)
        loadChannelProps(info, data, channel)

        val hierarchyOffset = readOffset(info, data)
        data.position(hierarchyOffset)

        loadBuffer(info, data, channel)

        return channel

    }

    private fun loadChannelProps(info: GimpInfo, data: ByteBuffer, channel: Channel) {
        while (true) {
            loadProp(info, data)
            when (info.propType) {
                PROP_OPACITY -> channel.opacity = data.int / 255f
                PROP_FLOAT_OPACITY -> channel.opacity = data.float
                PROP_COLOR_TAG -> channel.colorTag = data.int
                PROP_COLOR -> channel.color = rgba(data.get(), data.get(), data.get(), -1)
                PROP_FLOAT_COLOR -> channel.color = rgba(data.float, data.float, data.float, 1f)
                else -> skipUnknownProperty(info, data)
            }
        }
    }

    private fun readOffset(info: GimpInfo, data: ByteBuffer): Int {
        return if (info.bytesPerOffset == 4) data.int else data.long.toInt()
    }

    private fun loadProp(info: GimpInfo, data: ByteBuffer) {
        info.propType = data.int
        info.propSize = data.int
        println("prop ${info.propType}, size ${info.propSize}")
    }

    private fun loadImageProps(info: GimpInfo, data: ByteBuffer) {
        while (true) {
            loadProp(info, data)
            when (info.propType) {
                PROP_END -> break
                PROP_COLORMAP -> {
                    val size = data.int
                    val colors = if (info.fileVersion == 0) {
                        data.position(data.position() + size)
                        IntArray(size) { it * 0x10101 }
                        // data is already filled :)
                    } else {
                        IntArray(size) {
                            rgba(data.get(), data.get(), data.get(), 255.toByte())
                        }
                    }
                    if (info.imageType == GIMP_INDEXED) {
                        info.colorMap = colors
                    }
                }
                PROP_COMPRESSION -> {
                    when (val type = data.get().toInt()) {
                        COMPRESS_NONE, COMPRESS_RLE, COMPRESS_ZLIB,
                        COMPRESS_FRACTAL -> info.compression = type
                        else -> throw IOException("Unknown compression")
                    }
                }
                else -> {
                    skipUnknownProperty(info, data)
                }
            }
        }
    }

    private fun skipUnknownProperty(info: GimpInfo, data: ByteBuffer) {
        data.position(data.position() + info.propSize)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val bytes = getReference(OS.documents, "Watch Dogs 2 Background.xcf").readByteBuffer()
            .order(ByteOrder.BIG_ENDIAN)
        loadStream(bytes)
    }


}