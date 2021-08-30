package me.anno.image.webp

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.image.webp.LZ77.codeLengthCodeOrder
import me.anno.image.webp.LZ77.lz77DistanceOffsets
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.LOGGER
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

class WebpReader {

    val HUFF_IDX_GREEN = 0
    val HUFF_IDX_RED = 1
    val HUFF_IDX_BLUE = 2
    val HUFF_IDX_ALPHA = 3
    val HUFF_IDX_DIST = 4
    val HUFFMAN_CODES_PER_META_CODE = 5

    val NUM_CODE_LENGTH_CODES = 19

    class BitReader(val buffer: ByteBuffer) {
        var remaining = 0
        var data = 0L
        fun read1(): Boolean = read(1) != 0
        fun read(n: Int): Int {
            while (n > remaining) {
                remaining += 8
                data = (data shl 8) + buffer.get()
            }
            val delta = remaining - n
            val mask = ((1 shl n) - 1)
            val value = (data shr delta).toInt() and mask
            remaining -= n
            return value
        }
    }

    class BitReader2(val buffer: IntArray, var offset: Int) {
        var remaining = 0
        var data = 0L
        fun read1(): Boolean = read(1) != 0
        fun read(n: Int): Int {
            while (n > remaining) {
                remaining += 32
                data = (data shl 32) + buffer[offset++]
            }
            val delta = remaining - n
            val mask = ((1 shl n) - 1)
            val value = (data shr delta).toInt() and mask
            remaining -= n
            return value
        }
    }

    fun vp8LossyDecodeFrame(data: ByteBuffer): Image {
        // format: 420p
        TODO()
    }

    var image: IntImage? = null
    var predictor: Image? = null

    var predictorSizeReduction = 0

    var w = 0
    var h = 0

    fun vp8LosslessDecodeFrame(
        data: ByteBuffer,
        isAlphaChunk: Boolean
    ) {

        // var isLossless = false
        var hasAlpha = isAlphaChunk

        /*if (!isAlphaChunk) {
            isLossless = true
            format = BufferedImage.TYPE_INT_ARGB
        }*/

        val bits = BitReader(data)

        if (!isAlphaChunk) {

            if (bits.read(8) != 0x2f) throw IOException("Invalid Webp lossless signature")

            w = bits.read(14) + 1
            h = bits.read(14) + 1

            hasAlpha = bits.read1()

            if (bits.read(3) != 0) throw IOException("Invalid Webp lossless version")

        } else {
            if (w == 0 || h == 0) throw IOException("Missing size")
        }

        // parse transformations
        var numTransforms = 0
        var reducedWidth = 0
        var used = 0

        if (image == null) image = IntImage(w, h, IntArray(w * h), hasAlpha)

        val transforms = IntArray(4)

        while (bits.read1()) {
            val transform = bits.read(2)
            if (used.and(1 shl transform) != 0) {
                throw IOException("Transform $transform used more than once")
            }
            used = used or (1 shl transform)
            transforms[numTransforms++] = transform
            when (transform) {
                PREDICTOR_TRANSFORM -> parseTransformPredictor(bits)
                COLOR_TRANSFORM -> parseTransformColor(bits)
                COLOR_INDEXING_TRANSFORM -> parseTransformColorIndexing(bits)
            }
        }

        // decode primary image
        for (i in numTransforms - 1 downTo 0) {
            when (transforms[i]) {
                PREDICTOR_TRANSFORM -> applyPredictorTransform()
                COLOR_TRANSFORM -> applyColorTransform()
                SUBTRACT_GREEN -> applyMakeGrayscaleTransform()
                COLOR_INDEXING_TRANSFORM -> applyColorIndexingTransform()
            }
        }

    }

    var indexing: IntImage? = null
    var indexingSizeReduction = 0

    var colorTransform: IntImage? = null
    var colorTransformSizeReduction = 0

    // idk... but blue must be 2...
    val b = 2
    val g = 3
    val r = 1
    val a = 0

    fun parseTransformPredictor(bits: BitReader) {
        parseBlockSize(w, bits)
        predictor = decodeEntropyEncodedImage(bits, blocksW, blocksH, false)
        predictorSizeReduction = blockBits
    }

    // ff_u8_to_s8
    fun makeByteSigned(unsignedValue: Int): Int = unsignedValue.toByte().toInt()

    fun colorTransformDelta(predicate: Int, color: Int): Int {
        return (makeByteSigned(predicate) * makeByteSigned(color)) shr 5
    }

    var blocksW = 0
    var blocksH = 0
    var blockBits = 0

    var indexingReducedWidth = 0

    fun align(w: Int, w2: Int): Int {
        return (w + w2 - 1) / w2 * w2
    }

    fun align2(w: Int, w2: Int): Int {
        return (w + w2 - 1) / w2
    }

    fun align3(w: Int, bits: Int): Int {
        return (w + (1 shl bits) - 1) shr bits
    }

    fun parseBlockSize(w: Int, bits: BitReader) {
        blockBits = bits.read(3) + 2
        blocksW = align3(w, blockBits)
        blocksH = align3(h, blockBits)
    }

    fun parseTransformColor(bits: BitReader) {
        parseBlockSize(w, bits)
        colorTransform = decodeEntropyEncodedImage(bits, blocksW, blocksH, false)
        colorTransformSizeReduction = blockBits
    }

    fun parseTransformColorIndexing(bits: BitReader) {

        val indexSize = bits.read(8) + 1
        val widthBits = when (indexSize) {
            in 1..2 -> 3
            in 3..4 -> 2
            in 5..16 -> 1
            else -> 0
        }

        indexing = decodeEntropyEncodedImage(bits, indexSize, 1, false)
        indexingSizeReduction = widthBits

        if (widthBits > 0) {
            indexingReducedWidth = align3(w, widthBits)
        }

        // color index values are delta encoded
        val data = indexing!!.data
        for (i in 1 until indexSize) {
            data[i] += data[i - 1]
        }

    }

    var numHuffmanGroups = 1

    var reducedWidth = 0

    val NUM_LITERAL_CODES = 256
    val NUM_LENGTH_CODES = 24
    val NUM_DISTANCE_CODES = 40
    val NUM_SHORT_DISTANCES = 20
    val MAX_HUFFMAN_CODE_LENGTH = 15

    val alphabetSizes = intArrayOf(
        NUM_LITERAL_CODES + NUM_LENGTH_CODES,
        NUM_LITERAL_CODES, NUM_LITERAL_CODES, NUM_LITERAL_CODES,
        NUM_DISTANCE_CODES
    )

    class VLC {

    }

    class HuffReader {
        var simple = true
        var numSymbols = 0
        var simpleSymbol0 = 0
        var simpleSymbol1 = 0
        var table: VLC? = null
    }

    fun webpGetVLC(bits: BitReader, table: VLC): Int {


        TODO()
    }

    fun huffReaderGetSymbol(r: HuffReader, bits: BitReader): Int {
        return if (r.simple) {
            when {
                r.numSymbols == 1 -> {
                    r.simpleSymbol0
                }
                bits.read1() -> r.simpleSymbol1
                else -> r.simpleSymbol0
            }
        } else webpGetVLC(bits, r.table!!)
    }

    fun colorCachePut(cache: IntArray, colorCacheBits: Int, c: Int) {
        val cacheIndex = (0x1E35A7BD * c) shr (32 - colorCacheBits)
        cache[cacheIndex] = c
    }

    var entropy: IntImage? = null
    var entropySizeReduction = 0

    fun getHuffmanGroup(x: Int, y: Int): Int {
        val entropy = entropy!!
        var group = 0
        if (entropySizeReduction > 0) {
            val groupX = x shr entropySizeReduction
            val groupY = y shr entropySizeReduction
            group = entropy.getRGB(groupX, groupY)
        }
        return group
    }

    fun readHuffmanCodeSimple(bits: BitReader, hc: HuffReader) {
        hc.numSymbols = bits.read(1) + 1
        hc.simpleSymbol0 = if (bits.read1()) {
            bits.read(8)
        } else {
            bits.read(1)
        }
        if (hc.numSymbols == 2) {
            hc.simpleSymbol1 = bits.read(8)
        }
        hc.simple = true
    }

    fun readHuffmanCodeNormal(bits: BitReader, hc: HuffReader, alphabetSize: Int) {
        val codeLenHC = HuffReader()
        val codeLengthCodeLengths = IntArray(NUM_CODE_LENGTH_CODES)
        val numCodes = 4 + bits.read(4)
        if (numCodes > NUM_CODE_LENGTH_CODES) throw IOException("Invalid data")
        for (i in 0 until numCodes) {
            codeLengthCodeLengths[codeLengthCodeOrder[i]] = bits.read(3)
        }

        huffReaderBuildCanonical(codeLenHC, codeLengthCodeLengths, NUM_CODE_LENGTH_CODES)

        val codeLengths = IntArray(alphabetSize)
        var maxSymbol = 0
        if (bits.read1()) {
            val bits0 = 2 + 2 * bits.read(3)
            maxSymbol = 2 + bits.read(bits0)
            if (maxSymbol > alphabetSize) throw IOException("Max Symbol > alphabet size")
        } else maxSymbol = alphabetSize

        var prevCodeLen = 8
        var symbol = 0
        while (symbol < alphabetSize) {
            val codeLen = huffReaderGetSymbol(codeLenHC, bits)
            if (codeLen < 16) {
                // literal code length
                codeLengths[symbol++] = codeLen
                if (codeLen != 0) prevCodeLen = codeLen
            } else {
                var repeat = 0
                var length = 0
                when (codeLen) {
                    16 -> {
                        repeat = 3 + bits.read(2)
                        length = prevCodeLen
                    }
                    17 -> repeat = 3 + bits.read(3)
                    18 -> repeat = 11 + bits.read(7)
                }
                if (symbol + repeat > alphabetSize) throw IOException("Invalid symbol + repeat > alphabet size")
                for (i in 0 until repeat) {
                    codeLengths[symbol++] = length
                }
            }
        }

        huffReaderBuildCanonical(hc, codeLengths, alphabetSize)

    }

    fun huffReaderBuildCanonical(r: HuffReader, codeLengths: IntArray, alphabetSize: Int) {
        var len = 0
        var code = 0
        for (sym in 0 until alphabetSize) {
            if (codeLengths[sym] > 0) {
                len++
                code = sym
                if (len > 1) break
            }
        }
        if (len == 1) {
            r.numSymbols = 1
            r.simpleSymbol0 = code
            r.simple = true
            return
        }
        var maxCodeLength = 0
        for (sym in 0 until alphabetSize) {
            maxCodeLength = max(maxCodeLength, codeLengths[sym])
        }
        if (maxCodeLength == 0 || maxCodeLength > MAX_HUFFMAN_CODE_LENGTH) throw IOException()
        val codes = IntArray(alphabetSize)
        code = 0
        r.numSymbols = 0
        for (len2 in 1..maxCodeLength) {
            for (sym in 0 until alphabetSize) {
                if (codeLengths[sym] != len2) continue
                codes[sym] = code++
                r.numSymbols++
            }
            code = code shl 1
        }
        if (r.numSymbols == 0) throw IOException("Invalid data")
        TODO()//initVLC(r.table, 8, alphabetSize, codeLengths, codes, 0)
        r.simple = false
    }

    fun decodeEntropyImage(bits: BitReader) {
        var width = w
        if (reducedWidth > 0) width = reducedWidth
        parseBlockSize(width, bits)
        entropy = decodeEntropyEncodedImage(bits, blocksW, blocksH, false)
        entropySizeReduction = blockBits
        // the number of huffman groups is determined by the maximum group number
        // coded in the entropy image
        val data = entropy!!.data
        var maxGroup = 0
        for (i in 0 until blocksW * blocksH) {
            maxGroup = max(maxGroup, data[i])
        }
        numHuffmanGroups = maxGroup + 1
    }

    fun decodeEntropyEncodedImage(bits: BitReader, w: Int, h: Int, hasRoleARGB: Boolean): IntImage {

        val data = IntArray(w * h)
        val image = IntImage(w, h, data, false)

        var colorCache: IntArray? = null
        var colorCacheBits = 0

        if (bits.read1()) {
            colorCacheBits = bits.read(4)
            if (colorCacheBits < 1 || colorCacheBits > 11) throw IOException("Invalid color cache bits")
            colorCache = IntArray(1 shl colorCacheBits)
        }

        if (hasRoleARGB && bits.read1()) {
            decodeEntropyImage(bits)
            // numHuffmanGroups = s.numHuffmanGroups
        }

        val huffmanGroups = Array(numHuffmanGroups * HUFFMAN_CODES_PER_META_CODE) {
            HuffReader()
        }

        for (i in 0 until numHuffmanGroups) {
            val i2 = i * HUFFMAN_CODES_PER_META_CODE
            for (j in 0 until HUFFMAN_CODES_PER_META_CODE) {
                var alphabetSize = alphabetSizes[j]
                if (j == 0 && colorCacheBits > 0) {
                    alphabetSize += 1 shl colorCacheBits
                }
                val hg = huffmanGroups[i2 + j]
                if (bits.read1()) {
                    readHuffmanCodeSimple(bits, hg)
                } else {
                    readHuffmanCodeNormal(bits, hg, alphabetSize)
                }
            }
        }

        var width = w
        if (hasRoleARGB && reducedWidth > 0) {
            width = reducedWidth
        }

        var x = 0
        var y = 0
        val height = h
        while (y < height) {
            val i2 = getHuffmanGroup(x, y) * HUFFMAN_CODES_PER_META_CODE
            val v = huffReaderGetSymbol(huffmanGroups[i2 + HUFF_IDX_GREEN], bits)
            if (v < NUM_LITERAL_CODES) {
                // literal pixel values
                val idx = x + y * width
                val p2 = v + 0
                val p1 = huffReaderGetSymbol(huffmanGroups[i2 + HUFF_IDX_RED], bits)
                val p3 = huffReaderGetSymbol(huffmanGroups[i2 + HUFF_IDX_BLUE], bits)
                val p0 = huffReaderGetSymbol(huffmanGroups[i2 + HUFF_IDX_ALPHA], bits)
                val color = rgba(p1, p3, p2, p0)
                data[idx] = color
                if (colorCacheBits > 0) {
                    colorCachePut(colorCache!!, colorCacheBits, color)
                }
                x++
                if (x == width) {
                    x = 0
                    y++
                }
            } else if (v < NUM_LITERAL_CODES + NUM_LENGTH_CODES) {
                // LZ77 backwards mapping
                var prefixCode: Int = v - NUM_LITERAL_CODES
                val length = if (prefixCode < 4) {
                    prefixCode + 1
                } else {
                    val extraBits = (prefixCode - 2) shr 1
                    val offset = (2 + (prefixCode and 1)) shl extraBits
                    offset + bits.read(extraBits) + 1
                }
                prefixCode = huffReaderGetSymbol(huffmanGroups[i2 + HUFF_IDX_DIST], bits)
                if (prefixCode > 39) throw IOException("Distance prefix code too large: $prefixCode")
                var distance = if (prefixCode < 4) {
                    prefixCode + 1
                } else {
                    val extraBits = (prefixCode - 2) shr 1
                    val offset = (2 + (prefixCode and 1)) shl extraBits
                    offset + bits.read(extraBits) + 1
                }
                // find reference location
                if (distance <= NUM_SHORT_DISTANCES) {
                    val offset = lz77DistanceOffsets[distance - 1]
                    val xi = offset[0]
                    val yi = offset[1]
                    distance = max(1, xi + yi * width)
                } else {
                    distance -= NUM_SHORT_DISTANCES
                }
                var refX = x
                var refY = y
                if (distance <= x) {
                    refX -= distance
                    distance = 0
                } else {
                    refX = 0
                    distance -= x
                }
                while (distance >= width) {
                    refY--
                    distance -= width
                }
                if (distance > 0) {
                    refX = width - distance
                    refY--
                }
                refX = max(0, refX)
                refY = max(0, refY)

                // copy pixels
                // source and destination can overlap :/
                for (i in 0 until length) {
                    val color = data[refX + refY * width]
                    data[x + y * width] = color
                    if (colorCacheBits != 0) {
                        colorCachePut(colorCache!!, colorCacheBits, color)
                    }
                    x++
                    refX++
                    if (x == width) {
                        x = 0
                        y++
                    }
                    if (refX == width) {
                        refX = 0
                        refY++
                    }
                    if (y == height || refY == height) {
                        break
                    }
                }
            } else {
                // read from color cache
                val cacheIndex = v - (NUM_LITERAL_CODES + NUM_LENGTH_CODES)
                image.setRGB(x, y, colorCache!![cacheIndex])
                x++
                if (x == width) {
                    x = 0
                    y++
                }
            }
        }

        TODO()
    }

    fun applyColorTransform() {
        val image = image!!
        val transform = colorTransform!!
        val sizeReduction = colorTransformSizeReduction
        for (y in 0 until h) {
            for (x in 0 until w) {
                val cx = x shr sizeReduction
                val cy = y shr sizeReduction
                val cp = transform.getRGB(cx, cy)
                var color = image.getRGB(x, y)
                val blue = color.b()
                color += colorTransformDelta(cp.r(), blue) shl (g * 8)
                color += (colorTransformDelta(cp.b(), blue) +
                        colorTransformDelta(cp.g(), color.g())) shl (r * 8)
            }
        }
    }

    // "subtract green", in reality it makes the image grayscale
    fun applyMakeGrayscaleTransform() {
        val image = image!!
        val w = w
        val h = h
        val black = 255 shl 24
        for (y in 0 until h) {
            for (x in 0 until w) {
                // do we need to keep the alpha channel???
                val argb = image.getRGB(x, y)
                val blue = argb.and(255)
                image.setRGB(x, y, argb.and(black) + blue * 0x10101)
            }
        }
    }

    fun applyColorIndexingTransform() {

        val image = image!!
        val indexing = indexing!!

        val w = w
        val h = h

        val data = image.data

        if (indexingSizeReduction > 0) {
            val pixelBits = 8 shr indexingSizeReduction
            val line = IntArray(w)
            val reader = BitReader2(line, 0)
            for (y in 0 until h) {
                // copy the line from p into line
                val pi = y * w
                System.arraycopy(data, pi, line, 0, w)
                reader.offset = 0
                reader.remaining = 0
                reader.read(24) // skip alpha, red, green
                val shift = 1 shl indexingSizeReduction
                var i = 0
                for (x in 0 until w) {
                    // read as many pixels as we need for the indexed mode
                    data[pi + x] = reader.read(pixelBits)
                    if (++i == shift) {
                        reader.read(24) // jump over arg, only use b
                        i = 0
                    }
                }
            }
        }

        val palette = indexing.data
        // out of bounds entries are black -> just allocate them as black
        for (i in 0 until w * h) {
            data[i] = palette[data[i]]
        }

    }

    fun inversePrediction(image: IntImage, mode: Int, x: Int, y: Int) {
        val dec = image.getRGB(x, y)
        val left = image.getRGB(x - 1, y)
        val topLeft = image.getRGB(x - 1, y - 1)
        val top = image.getRGB(x, y - 1)
        val topRight = if (x == w - 1) image.getRGB(0, y) else image.getRGB(x + 1, y - 1)
        val pixel = when (mode) {
            0 -> 0xff shl 24
            1 -> left
            2 -> top
            3 -> topRight
            4 -> topLeft
            5 -> rgba(
                (top.r() + (left.r() + topRight.r()).shr(1)).shr(1),
                (top.g() + (left.g() + topRight.g()).shr(1)).shr(1),
                (top.b() + (left.b() + topRight.b()).shr(1)).shr(1),
                (top.a() + (left.a() + topRight.a()).shr(1)).shr(1),
            )
            6 -> rgba(
                (top.r() + topLeft.r()).shr(1),
                (top.g() + topLeft.g()).shr(1),
                (top.b() + topLeft.b()).shr(1),
                (top.a() + topLeft.a()).shr(1),
            )
            7 -> rgba(
                (topLeft.r() + top.r()).shr(1),
                (topLeft.g() + top.g()).shr(1),
                (topLeft.b() + top.b()).shr(1),
                (topLeft.a() + top.a()).shr(1),
            )
            8 -> rgba(
                (topLeft.r() + top.r()).shr(1),
                (topLeft.g() + top.g()).shr(1),
                (topLeft.b() + top.b()).shr(1),
                (topLeft.a() + top.a()).shr(1)
            )
            9 -> rgba(
                (top.r() + topRight.r()).shr(1),
                (top.g() + topRight.g()).shr(1),
                (top.b() + topRight.b()).shr(1),
                (top.a() + topRight.a()).shr(1)
            )
            10 -> rgba(
                ((left.r() + topLeft.r()).shr(1) + (top.r() + topRight.r()).shr(1)).shr(1),
                ((left.g() + topLeft.g()).shr(1) + (top.g() + topRight.g()).shr(1)).shr(1),
                ((left.b() + topLeft.b()).shr(1) + (top.b() + topRight.b()).shr(1)).shr(1),
                ((left.a() + topLeft.a()).shr(1) + (top.a() + topRight.a()).shr(1)).shr(1),
            )
            11 -> {
                val diff = abs(left.r() - topLeft.r()) - abs(top.r() - topLeft.r()) +
                        abs(left.g() - topLeft.g()) - abs(top.g() - topLeft.g()) +
                        abs(left.b() - topLeft.b()) - abs(top.b() - topLeft.b()) +
                        abs(left.a() - topLeft.a()) - abs(top.a() - topLeft.a())
                if (diff <= 0) top
                else left
            }
            12 -> rgba(
                clip8(left.r() + top.r() - topLeft.r()),
                clip8(left.g() + top.g() - topLeft.g()),
                clip8(left.b() + top.b() - topLeft.b()),
                clip8(left.a() + top.a() - topLeft.a()),
            )
            13 -> rgba(
                clampAddSubHalf(left.r(), top.r(), topLeft.r()),
                clampAddSubHalf(left.g(), top.g(), topLeft.g()),
                clampAddSubHalf(left.b(), top.b(), topLeft.b()),
                clampAddSubHalf(left.a(), top.a(), topLeft.a()),
            )
            else -> 0
        }
        image.setRGB(x, y, dec + pixel)
    }

    fun clampAddSubHalf(left: Int, top: Int, topLeft: Int): Int {
        val d = (left + top).shr(1)
        return clip8(d + (d - topLeft) / 2)
    }

    fun clip8(x: Int): Int {
        return if (x < 0) 0 else if (x < 255) x else 255
    }

    fun applyPredictorTransform() {
        val sizeReduction = predictorSizeReduction
        val predictor = predictor!!
        val image = image!!
        for (y in 0 until h) {
            for (x in 0 until w) {
                val tx = x shr sizeReduction
                val ty = y shr sizeReduction
                var m = predictor.getRGB(tx, ty).r() // channel 2
                if (x == 0) {
                    m = if (y == 0) PRED_MODE_BLACK else PRED_MODE_T
                } else if (y == 0) {
                    m = PRED_MODE_L
                }
                if (m > 13) throw IOException("Invalid predictor mode")
                inversePrediction(image, m, x, y)
            }
        }
    }

    val PRED_MODE_BLACK = 0
    val PRED_MODE_L = 1
    val PRED_MODE_T = 2

    val PREDICTOR_TRANSFORM = 0
    val COLOR_TRANSFORM = 1
    val SUBTRACT_GREEN = 2
    val COLOR_INDEXING_TRANSFORM = 3

    fun getLE24(data: ByteBuffer): Int {
        return data.get().toInt().and(255) or
                data.get().toInt().and(255).shl(8) or
                data.get().toInt().and(255).shl(16)
    }

    fun read(data: ByteBuffer): Image? {

        if (data.int != RIFF) throw IOException("Missing RIFF Tag")

        var chunkSize = data.int
        if (data.remaining() < chunkSize) throw IOException("Invalid data")

        if (data.int != WEBP) throw IOException("Missing WEBP Tag")

        var hasIccpTag = false

        var image: Image? = null

        var width = 0
        var height = 0
        var hasAlpha = false

        while (data.remaining() > 8) {

            val chunkType = data.int
            chunkSize = data.int

            if (chunkSize == -1) throw IOException("Invalid data")

            chunkSize += chunkSize.and(1)

            if (data.remaining() < chunkSize) throw IOException("Invalid data")

            val endPosition = data.position() + chunkSize

            when (chunkType) {
                VP8 -> if (image == null) image = vp8LossyDecodeFrame(data)
                VP8L -> if (image == null) vp8LosslessDecodeFrame(data, false)
                VP8X -> {
                    if (image == null && width == 0 && height == 0) {
                        val flags = data.get()
                        data.position(data.position() + 3)
                        width = getLE24(data) + 1
                        height = getLE24(data) + 1

                    } else throw RuntimeException("Already got dimensions")
                }
                ALPHA_CHUNK -> TODO()
                EXIF -> TODO()
                ICCP -> {
                    if (hasIccpTag) {
                        LOGGER.warn("Skipping second ICCP chunk")
                    } else {
                        hasIccpTag = true
                        TODO()
                    }
                }
                ANIM, ANIX, XMP -> LOGGER.warn("Skipping unsupported chunk $chunkType")
                else -> LOGGER.warn("Skipping unknown chunk $chunkType")
            }

            data.position(endPosition)

        }

        return image

    }

    companion object {

        val RIFF = leTag("RIFF")
        val WEBP = leTag("WEBP")
        val VP8 = leTag("VP8 ")
        val VP8L = leTag("VP8L")
        val VP8X = leTag("VP8X")
        val ALPHA_CHUNK = leTag("ALPH")
        val EXIF = leTag("EXIF")
        val ICCP = leTag("ICCP")
        val ANIM = leTag("ANIM")
        val ANIX = leTag("ANMP")
        val XMP = leTag("XMP ")

        fun leTag(str: String): Int {
            return str[0].code or str[1].code.shl(8) or str[2].code.shl(16) or str[3].code.shl(24)
        }

    }

}