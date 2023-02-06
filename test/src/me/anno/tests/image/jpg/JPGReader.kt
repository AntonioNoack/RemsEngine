package me.anno.tests.image.jpg

import me.anno.image.raw.ByteImage
import me.anno.io.Streams.readBE16
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.InputStreams.skipN
import java.io.*
import kotlin.math.max

// https://github.com/ImageMagick/ImageMagick/blob/80bd592dd20d4a1287842459ca3801c3ae3866cd/coders/jpeg.c
// which uses https://github.com/leapmotion/libjpeg-turbo
// https://github.com/nothings/stb/blob/master/stb_image.h

// the standard readers work well and good, but unfortunately,
//  then don't allow me to load the image as a small size;
//  which should definitively be possible
// todo is there a way to read jpg thumbnails from the primary data?
// writing our own reader is MUCH to complicated; jpeg is insane
class JPGReader {

    companion object {

        const val MARKER_NONE = 255
        const val FAST_BITS = 4 // larger = more cases, smaller = less cache usage
        const val FAST_SIZE = 1 shl FAST_BITS
        const val FAST_MASK = FAST_SIZE - 1

        @JvmStatic
        fun main(args: Array<String>) {
            fun convert(file: FileReference) {
                if (file.isDirectory) {
                    for (child in file.listChildren() ?: return) {
                        convert(child)
                    }
                } else if (Signature.findNameSync(file) == "jpg") {
                    try {
                        val input = file.inputStreamSync()
                        JPGReader().read(input)
                            .write(desktop.getChild("jpg/${file.nameWithoutExtension}.png"))
                        input.close()
                        // throw IOException("done :)")
                    } catch (e: IOException) {
                        // throw
                        IOException(file.name, e)
                            .printStackTrace()
                    }
                }
            }

            val out = System.out
            val out2 = documents.getChild("IdeaProjects/VideoStudio/src/me/anno/image/jpg/log0.txt")
                .outputStream()
            System.setOut(PrintStream(object : OutputStream() {
                override fun write(p0: Int) {
                    out.write(p0)
                    out2.write(p0)
                }
            }))
            // not working:
            // broken at 50%:
            // convert(pictures.getChild("Anime/E0mfHayVUAElktk.jpg"))
            // broken at 50% as well:
            // convert(pictures.getChild("Anime/FIzuEaIaAAgU6ns.jpg"))
            // convert(pictures.getChild("create-professional-green-screen-spokesperson-video_background.jpg"))
            // convert(pictures.getChild("jpg-test-0.jpg"))
            // convert(pictures.getChild("jpg-test-1.jpg"))
            // convert(pictures.getChild("Anime/90940211_p0_master1200.jpg"))
            // convert(pictures.getChild("Anime/img.jpg"))
            // convert(pictures.getChild("small-bg.jpg"))
            // convert(pictures.getChild("small-bg2.jpg"))
            // convert(pictures.getChild("small-bg3.jpg"))
            // convert(pictures.getChild("small-bg3-2.jpg"))
            // todo there is a bug with files > 64kB
            // todo print all statements into a huge file, and diff it!
            convert(pictures.getChild("small-bg3-3.jpg"))
            // convert(pictures)
            out2.close()
        }

        @JvmStatic
        private val zigzag = byteArrayOf(
            0, 1, 8, 16, 9, 2, 3, 10,
            17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63,
            63, 63, 63, 63, 63, 63, 63, 63,
            63, 63, 63, 63, 63, 63, 63
        )

        @JvmStatic
        fun bMask(i: Int) = (1 shl i) - 1

        @JvmStatic
        private fun jBias(i: Int) = ((-1) shl i) + 1

        @JvmStatic
        private fun f2f(f: Float) = (f * 4096 + 0.5f).toInt()

        @JvmStatic
        private fun fsh(i: Int) = i * 4096

        private val k0 = f2f(0.5411961f)
        private val k1 = f2f(-1.847759f)
        private val k2 = f2f(0.76536685f)
        private val k3 = f2f(1.1758755f)
        private val k4 = f2f(0.29863134f)
        private val k5 = f2f(2.05312f)
        private val k6 = f2f(3.072711f)
        private val k7 = f2f(1.5013211f)
        private val k8 = f2f(-0.8999762f)
        private val k9 = f2f(-2.5629156f)
        private val k10 = f2f(-1.9615705f)
        private val k11 = f2f(-0.39018065f)

        @JvmStatic
        private fun f2f2(x: Float) = ((x * 4096 * 256) + 0.5f).toInt()

        private val k20 = +f2f2(1.40200f)
        private val k21 = -f2f2(0.71414f)
        private val k22 = -f2f2(0.34414f)
        private val k23 = +f2f2(1.77200f)

        @JvmStatic
        private fun clamp(x: Int): Byte {
            return if (x < 0) 0
            else if (x < 255) x.toByte()
            else -1
        }

        @JvmStatic
        fun Byte.u() = toInt().and(255)

        @JvmStatic
        private fun fast8x8(x: Int, m: Byte): Byte {
            val t = x * m.u() + 128
            return ((t + t.shr(8)) shr 8).toByte()
        }

        @JvmStatic
        private fun fast8x8(x: Byte, m: Byte): Byte {
            val t = x.u() * m.u() + 128
            return ((t + t.shr(8)) shr 8).toByte()
        }

        @JvmStatic
        private fun mad3SizesValid(a: Int, b: Int, c: Int) =
            a.toLong() * b.toLong() * c.toLong() <= Int.MAX_VALUE

    }

    var succHigh = 0
    var succLow = 0
    var specStart = 0
    var specEnd = 0
    val order = IntArray(4)

    var imgN = 0
    var rgb = 0
    var app14ColorTransform = 0
    var jfif = false
    var restartInterval = 0

    var imgMcuH = 0
    var imgMcuW = 0
    var imgHMax = 0
    var imgVMax = 0
    var imgY = 0
    var imgX = 0
    var marker = MARKER_NONE

    var scanN = 0
    var codeBits = 0
    var codeBuffer = 0
    var nomore = false
    var todo = 0
    var eobRun = 0
    var imgMcuX = 0
    var imgMcuY = 0

    val idct0 = ShortArray(64)
    val idct1 = IntArray(64)

    val imgComp = Array(4) { ImgComp() }

    val deq = Array(4) { IntArray(64) }
    val huffDc = Array(4) { Huffman() }
    val huffAc = Array(4) { Huffman() }
    val fastAc = Array(4) { ShortArray(FAST_SIZE) }

    var progressive = false

    fun yCbCrToRGB(
        out: ByteArray, outIndex0: Int,
        data: Array<ByteArray>, dataOffset: IntArray
    ) {
        val ya = data[0]
        val yi = dataOffset[0]
        val half = 1 shl 19
        val ra = data[1]
        val ri = dataOffset[1]
        val ba = data[2]
        val bi = dataOffset[2]
        var outIndex = outIndex0
        for (i in 0 until imgX) {
            val yFixed = (ya[yi + i].u() shl 20) + half
            val cr = ra[ri + i].u() - 128
            val cb = ba[bi + i].u() - 128
            // seems to be bgr...
            val b = (yFixed + cr * k20) shr 20
            val g = (yFixed + (cr * k21 + cb * k22)) shr 20
            val r = (yFixed + cb * k23) shr 20
            out[outIndex++] = clamp(r)
            out[outIndex++] = clamp(g)
            out[outIndex++] = clamp(b)
        }
    }

    fun read(input: InputStream): ByteImage {
        // load_jpeg_image
        // load an image, but leave in YCbCr format
        decodeImage(input)
        val n = imgN // 3 or 1
        val isRGB = n == 3 && (rgb == 3 || app14ColorTransform == 0 && !jfif)
        // resample and color-convert
        val resComp = Array(4) { Resample() }
        for (k in 0 until n) {
            val r = resComp[k]
            val comp = imgComp[k]

            // ByteImage(comp.w2, comp.h2, ByteImage.Format.R, comp.data)
            //    .write(desktop.getChild("jpg/layer$k.png"))

            comp.lineBuff = ByteArray(imgX + 3)
            r.hs = imgHMax / comp.h
            r.vs = imgVMax / comp.v
            r.yStep = r.vs shr 1
            r.w = ceilDiv(imgX, r.hs)
            r.yPos = 0
            r.data = comp.data
            r.line0 = 0
            r.line1 = 0
            r.resample = when {
                r.hs == 1 && r.vs == 1 -> ResampleRow1
                r.hs == 1 && r.vs == 2 -> ResampleRowV2
                r.hs == 2 && r.vs == 1 -> ResampleRowH2
                r.hs == 2 && r.vs == 2 -> ResampleHv2
                else -> ResampleRowGeneric
            }
        }
        val data2 = Array(n) { imgComp[it].data }
        val data2Off = IntArray(n)
        val out = ByteArray(n * imgX * imgY)
        var outChannels = n

        // println("rgb? $isRGB, channels: $n, transform: $app14ColorTransform")

        for (j in 0 until imgY) {
            var outIndex = n * imgX * j
            for (k in 0 until n) {
                val r = resComp[k]
                val yBot = r.yStep >= (r.vs shr 1)
                val comp = imgComp[k]
                val inNear = if (yBot) r.line1 else r.line0
                val inFar = if (yBot) r.line0 else r.line1
                // println("y: $j, k: $k")
                val v = r.resample.run(comp.lineBuff, r.data, inNear, inFar, r.w, r.hs)
                if (v < 0) {
                    data2[k] = comp.lineBuff
                    data2Off[k] = 0
                } else {
                    data2[k] = r.data
                    data2Off[k] = inNear
                }
                if (++r.yStep >= r.vs) {
                    r.yStep = 0
                    r.line0 = r.line1
                    if (+r.yPos < comp.y) {
                        r.line1 += comp.w2
                    }
                }
            }
            when (n) {
                1 -> System.arraycopy(data2[0], data2Off[0], out, outIndex, imgX)
                3 -> {
                    if (isRGB) {
                        val ra = data2[0]
                        val ri = data2Off[0]
                        val ga = data2[1]
                        val gi = data2Off[1]
                        val ba = data2[2]
                        val bi = data2Off[2]
                        for (i in 0 until imgX) {
                            out[outIndex++] = ra[ri + i]
                            out[outIndex++] = ga[gi + i]
                            out[outIndex++] = ba[bi + i]
                        }
                    } else {
                        yCbCrToRGB(out, outIndex, data2, data2Off)
                    }
                }
                4 -> {
                    when (app14ColorTransform) {
                        0 -> { // CMYK
                            val ra = data2[0]
                            val ri = data2Off[0]
                            val ga = data2[1]
                            val gi = data2Off[1]
                            val ba = data2[2]
                            val bi = data2Off[2]
                            val ma = data2[3]
                            val mi = data2Off[3]
                            outChannels = 3
                            for (i in 0 until imgX) {
                                val m = ma[mi + i]
                                out[outIndex++] = fast8x8(ra[ri + i], m)
                                out[outIndex++] = fast8x8(ga[gi + i], m)
                                out[outIndex++] = fast8x8(ba[bi + i], m)
                            }
                        }
                        2 -> {// YCCK
                            outChannels = 3
                            yCbCrToRGB(out, outIndex, data2, data2Off)
                            val ma = data2[3]
                            val mi = data2Off[3]
                            for (i in 0 until imgX) {
                                val m = ma[mi + i]
                                out[outIndex] = fast8x8(255 - out[outIndex++].u(), m)
                                out[outIndex] = fast8x8(255 - out[outIndex++].u(), m)
                                out[outIndex] = fast8x8(255 - out[outIndex++].u(), m)
                            }
                        }
                        else -> {
                            // YCbCr + alpha? alpha is currently ignored
                            outChannels = 3
                            yCbCrToRGB(out, outIndex, data2, data2Off)
                        }
                    }
                }
            }
        }
        return ByteImage(
            imgX, imgY, when (outChannels) {
                1 -> ByteImage.Format.R
                3 -> ByteImage.Format.RGB
                else -> ByteImage.Format.RGBA
            }, out
        )
    }

    fun decodeHeader(input: InputStream) {
        jfif = false
        app14ColorTransform = -1
        marker = MARKER_NONE
        if (getMarker(input) != 0xd8) throw IOException("no SOI") // start of image
        var m = getMarker(input)
        while (m !in 0xc0..0xc2) {
            if (!processMarker(input, m)) return
            m = getMarker(input)
            while (m == MARKER_NONE) {
                m = getMarker(input)
            }
        }
        progressive = (m == 0xc2)
        // process frame header
        val lf = input.readBE16()
        if (lf < 11) throw IOException("bad SOF len")
        val p = input.read()
        if (p != 8) throw IOException("only 8-bit jpeg is supported")
        imgY = input.readBE16()
        if (imgY == 0) throw IOException("no header height")
        imgX = input.readBE16()
        if (imgX == 0) throw IOException("no header width")
        val c = input.read()
        if (c != 1 && c != 3 && c != 4) throw IOException("bad component count")
        imgN = c
        if (lf != 8 + 3 * imgN) throw IOException("bad SOF len")
        rgb = 0
        for (i in 0 until imgN) {
            val rgb = "RGB"
            val comp = imgComp[i]
            comp.id = input.read()
            // println("$i -> ${comp.id} -> ${comp.id.toChar()}")
            if (imgN == 3 && comp.id == rgb[i].code) {
                this.rgb++
            }
            val q = input.read()
            comp.h = q shr 4
            if (comp.h !in 1..4) throw IOException("bad H")
            comp.v = q and 15
            if (comp.v !in 1..4) throw IOException("bad V")
            comp.tqIndex = input.read()
            if (comp.tqIndex > 3) throw IOException("bad TQ")
        }
        if (!mad3SizesValid(imgX, imgY, imgN))
            throw IOException("image too large")
        var hMax = 0
        var vMax = 0
        for (i in 0 until imgN) {
            val comp = imgComp[i]
            hMax = max(hMax, comp.h)
            vMax = max(vMax, comp.v)
        }
        for (i in 0 until imgN) {
            val comp = imgComp[i]
            if (hMax % comp.h != 0) throw IOException("bad H")
            if (vMax % comp.v != 0) throw IOException("bad V")
        }
        imgHMax = hMax
        imgVMax = vMax
        imgMcuW = hMax * 8
        imgMcuH = vMax * 8
        imgMcuX = ceilDiv(imgX, hMax * 8)
        imgMcuY = ceilDiv(imgY, vMax * 8)
        // println("h,v-max: $hMax,$vMax, mcu: $imgMcuX,$imgMcuY,$imgMcuW,$imgMcuW")
        for (i in 0 until imgN) {
            val comp = imgComp[i]
            comp.x = ceilDiv(imgX * comp.h, hMax)
            comp.y = ceilDiv(imgY * comp.v, vMax)
            comp.w2 = imgMcuX * comp.h * 8
            comp.h2 = imgMcuY * comp.v * 8
            // comp.coeff = null
            // rawCoeff = null
            // lineBuff = null
            comp.data = ByteArray(comp.w2 * comp.h2)
            // println("[$i] ${comp.h},${comp.v} -> ${comp.x},${comp.y},${comp.w2},${comp.h2}")
            if (progressive) {
                comp.coeffW = comp.w2 / 8
                comp.coeffH = comp.h2 / 8
                comp.coeff = ShortArray(comp.w2 * comp.h2)
            }
        }
    }

    fun getMarker(input: InputStream): Int {
        if (marker != MARKER_NONE) {
            val x = marker
            marker = MARKER_NONE
            return x
        }
        var x = input.read()
        if (x != MARKER_NONE) return MARKER_NONE // ???
        while (x == MARKER_NONE) { // consume all -1 fill bytes
            x = input.read()
            if (x < 0) throw EOFException()
        }
        return x
    }

    private fun buildFastAc(fastAc: ShortArray, h: Huffman) {
        for (i in 0 until FAST_SIZE) {
            val fast = h.fast[i].u()
            fastAc[i] = 0
            if (fast < 255) {
                val rs = h.values[fast].toInt() and 0xff
                val run = (rs shr 4) and 15
                val magBits = rs and 15
                val len = h.size[fast].toInt() and 0xff
                if (magBits != 0 && len + magBits <= FAST_BITS) {
                    // magnitude followed by extend code
                    var k = ((i shl len) and FAST_MASK) shr (FAST_BITS - magBits)
                    val m = 1 shl (magBits - 1)
                    if (k < m) k += ((-1) shl magBits) + 1
                    if (k >= -128 && k <= 127) {
                        fastAc[i] = (k * 256 + run * 16 + len + magBits).toShort()
                    }
                }
            }
        }
    }

    fun processMarker(input: InputStream, m: Int): Boolean {
        println("reading marker: ${m.toString(16)}")
        when (m) {
            MARKER_NONE -> throw IllegalArgumentException()
            0xdd -> { // dri, specify restart interval
                if (input.readBE16() != 4) throw IOException("bad DRI len")
                restartInterval = input.readBE16()
                return true
            }
            0xdb -> { // dqt, define quantization table
                var l = input.readBE16() - 2
                while (l > 0) {
                    val q = input.read()
                    val p = q shr 4
                    if (p > 1) throw IOException("bad DQT type")
                    val t = q and 15
                    if (t > 3) throw IOException("bad DQT table")
                    val sixteen = p != 0
                    val table = deq[t]
                    if (sixteen) {
                        for (i in 0 until 64) {
                            table[zigzag[i].toInt()] = input.readBE16()
                        }
                    } else {
                        for (i in 0 until 64) {
                            table[zigzag[i].toInt()] = input.read()
                        }
                    }
                    l -= if (sixteen) 129 else 65
                }
                return l == 0
            }
            0xc4 -> { // dht, define huffman table
                var l = input.readBE16() - 2
                val sizes = IntArray(16)
                while (l > 0) {
                    val q = input.read()
                    val tc = q shr 4
                    val th = q and 15
                    if (tc > 1 || th > 3) throw IOException("bad DHT header")
                    var n = 0
                    for (i in 0 until 16) {
                        sizes[i] = input.read()
                        n += sizes[i]
                    }
                    l -= 17
                    val table = (if (tc == 0) huffDc else huffAc)[th]
                    table.build(sizes)
                    val v = table.values
                    for (i in 0 until n) {
                        v[i] = input.read().toByte()
                    }
                    if (tc != 0) {
                        buildFastAc(fastAc[th], huffAc[th])
                    }
                    l -= n
                }
                return l == 0
            }
            in 0xe0..0xef, 0xfe -> { // check for comment block or APP blocks
                var l = input.readBE16()
                if (l < 2) {
                    if (m == 0xfe) throw IOException("bad COM len")
                    else throw IOException("bad APP len")
                }
                l -= 2
                if (m == 0xe0 && l >= 5) { // jfif app0 segment
                    var ok = true
                    for (i in 0 until 5) {
                        if (input.read() != "JFIF\u0000"[i].code)
                            ok = false
                    }
                    l -= 5
                    if (ok) jfif = true
                } else if (m == 0xee && l >= 12) { // adobe app14 segment
                    var ok = true
                    for (i in 0 until 6) {
                        if (input.read() != "Adobe\u0000"[i].code)
                            ok = false
                    }
                    l -= 6
                    if (ok) {
                        input.read() // version
                        input.readBE16() // flags 0
                        input.readBE16() // flags 1
                        app14ColorTransform = input.read() // color transform
                        l -= 6
                    }
                }
                input.skipN(l.toLong())
                return true
            }
            else -> throw IOException("Unknown marker $m")
        }
    }

    fun decodeImage(input: InputStream) {
        // decode image to YCbCr format
        restartInterval = 0
        decodeHeader(input) // with scan load
        var m = getMarker(input)
        eof@ while (m != 0xd9 && m != 0) { // eof
            if (m == 0xda) {
                // println("parsing sos")
                processScanHeader(input)
                parseEntropyEncodedData(input)
                // handle 0s at the end of image data
                if (marker == MARKER_NONE) {
                    while (true) {
                        val x = input.read()
                        if (x < 0) break
                        if (x == 255) {
                            marker = input.read()
                            break
                        }
                    }
                }
            } else if (m == 0xdc) {
                val ld = input.readBE16()
                val nl = input.readBE16()
                if (ld != 4) throw IOException("bad DNL")
                if (nl != imgY) throw IOException("bad DNL height")
            } else {
                if (!processMarker(input, m)) {
                    throw IOException("Failed to process marker")
                }
            }
            m = getMarker(input)
        }
        if (progressive) {
            // dequantize and idct the data
            // println("filled progressive data")
            for (n in 0 until imgN) {
                val comp = imgComp[n]
                val w = (comp.x + 7) shr 3
                val h = (comp.y + 7) shr 3
                val data = comp.coeff
                for (j in 0 until h) {
                    for (i in 0 until w) {
                        val dataOffset = 64 * (i + j * comp.coeffW)
                        val deq = deq[comp.tqIndex]
                        for (k in 0 until 64) {
                            data[k + dataOffset] = (data[k + dataOffset] * deq[k]).toShort()
                        }
                        inverseDiscreteCosineTransform(comp.data, comp.w2 * j * 8 + i * 8, comp.w2, data, dataOffset)
                    }
                }
            }
        }
    }

    fun processScanHeader(input: InputStream) {
        val ls = input.readBE16()
        scanN = input.read()
        if (scanN < 1 || scanN > 4 || scanN > imgN)
            throw IOException("bad SOS comp count")
        if (ls != 6 + 2 * scanN)
            throw IOException("bad SOS len")
        for (i in 0 until scanN) {
            val id = input.read()
            val q = input.read()
            var which = -1
            while (++which < imgN) {
                if (imgComp[which].id == id) break
            }
            if (which == imgN) throw IOException("invalid id")
            val comp = imgComp[which]
            comp.dcIndex = q shr 4
            comp.acIndex = q and 15
            if (comp.dcIndex > 3) throw IOException("bad DC huff")
            if (comp.acIndex > 3) throw IOException("bad AC huff")
            order[i] = which
        }
        specStart = input.read()
        specEnd = input.read()
        val aa = input.read()
        succHigh = aa shr 4
        succLow = aa and 15
        if (progressive) {
            if (specStart > 63 || specEnd > 63 || specStart > specEnd || succHigh > 13 || succLow > 13)
                throw IOException("bad SOS")
        } else {
            if (specStart != 0 || aa != 0) throw IOException("bad SOS")
            specEnd = 63
        }
        println("sos: $scanN, [${imgComp.joinToString { "[${it.id}, ${it.dcIndex}, ${it.acIndex}]" }}], [${order.joinToString()}], $specStart .. $specEnd, $succHigh/$succLow, $progressive")
    }

    fun reset() {
        // println("--- calling reset ---")
        codeBits = 0
        codeBuffer = 0
        nomore = false
        for (i in 0 until 4)
            imgComp[i].dcPred = 0
        marker = MARKER_NONE
        todo = if (restartInterval > 0) restartInterval else Int.MAX_VALUE
        eobRun = 0
    }

    fun restart(marker: Int) = marker in 0xd0..0xd7

    fun growBufferUnsafe(input: InputStream) {
        // println("growing buffer...")
        do {
            val b = if (nomore) 0 else input.read()
            if (b == 0xff) {
                var c = input.read()
                while (c == 0xff) c = input.read()
                if (c != 0) {
                    marker = c
                    nomore = true
                    println("nomore")
                    return
                } else println("got c=0 after ff")
            }
            codeBuffer = codeBuffer or (b shl (24 - codeBits))
            codeBits += 8
            println("g$codeBuffer/$codeBits")
            // println("grew buffer, $codeBuffer/$codeBits")
        } while (codeBits <= 24)
    }

    fun parseEntropyEncodedData(input: InputStream) {
        reset()
        // println("progressive? $progressive, $scanN")
        if (!progressive) {
            if (scanN == 1) {
                val n = order[0]
                val comp = imgComp[n]
                val w = (comp.x + 7) shr 3
                val h = (comp.y + 7) shr 3
                for (j in 0 until h) {
                    for (i in 0 until w) {
                        val ha = comp.acIndex
                        decodeBlock(input, idct0, huffDc[comp.dcIndex], huffAc[ha], fastAc[ha], n, deq[comp.tqIndex])
                        inverseDiscreteCosineTransform(comp.data, comp.w2 * j * 8 + i * 8, comp.w2, idct0, 0)
                        if (--todo <= 0) {
                            if (codeBits < 24) growBufferUnsafe(input)
                            if (!restart(marker)) return
                            reset()
                        }
                    }
                }
            } else {
                // interleaved
                for (j in 0 until imgMcuY) {
                    for (i in 0 until imgMcuX) {
                        for (k in 0 until scanN) {
                            val n = order[k]
                            val comp = imgComp[n]
                            for (y in 0 until comp.v) {
                                for (x in 0 until comp.h) {
                                    val x2 = (i * comp.h + x) * 8
                                    val y2 = (j * comp.v + y) * 8
                                    val ha = comp.acIndex
                                    val dc = huffDc[comp.dcIndex]
                                    decodeBlock(input, idct0, dc, huffAc[ha], fastAc[ha], n, deq[comp.tqIndex])
                                    inverseDiscreteCosineTransform(comp.data, comp.w2 * y2 + x2, comp.w2, idct0, 0)
                                }
                            }
                        }
                        if (--todo <= 0) {
                            if (codeBits < 24) growBufferUnsafe(input)
                            if (!restart(marker)) return
                            reset()
                        }
                    }
                }
            }
        } else {
            if (scanN == 1) {
                // println("reading coeff data")
                val n = order[0]
                val comp = imgComp[n]
                val w = (comp.x + 7) shr 3
                val h = (comp.y + 7) shr 3
                println("p1/$w/$h/$todo")
                for (j in 0 until h) {
                    for (i in 0 until w) {
                        val data = comp.coeff
                        val dataOffset = 64 * (i + j * comp.coeffW)
                        if (specStart == 0) decodeBlockProgDc(input, data, dataOffset, huffDc[comp.dcIndex], n)
                        else decodeBlockProgAc(input, data, dataOffset, huffAc[comp.acIndex], fastAc[comp.acIndex])
                        if (--todo <= 0) {
                            if (codeBits < 24) growBufferUnsafe(input)
                            if (!restart(marker)) return
                            reset()
                        }
                    }
                }
            } else {
                // println("decoding coeff data dc")
                for (j in 0 until imgMcuY) {
                    for (i in 0 until imgMcuX) {
                        for (k in 0 until scanN) {
                            val n = order[k]
                            val comp = imgComp[n]
                            for (y in 0 until comp.v) {
                                for (x in 0 until comp.h) {
                                    val x2 = (i * comp.h + x)
                                    val y2 = (j * comp.v + y)
                                    val data = comp.coeff
                                    val dataOffset = 64 * (x2 + y2 * comp.coeffW)
                                    decodeBlockProgDc(input, data, dataOffset, huffDc[comp.dcIndex], n)
                                }
                            }
                        }
                        if (--todo <= 0) {
                            if (codeBits < 24) growBufferUnsafe(input)
                            if (!restart(marker)) return
                            reset()
                        }
                    }
                }
            }
        }
    }

    private fun getBit(input: InputStream): Boolean {
        if (codeBits < 1) growBufferUnsafe(input)
        val k = codeBuffer
        codeBuffer = codeBuffer shl 1
        codeBits--
        // println("got bit $k -> ${(k ushr 31) != 0}")
        println("b${if ((k ushr 31) != 0) 1 else 0}")
        return (k ushr 31) != 0
    }

    private fun getBits(input: InputStream, n: Int): Int {
        if (codeBits < n) growBufferUnsafe(input)
        var k = leftRot(codeBuffer, n)
        val bMask = bMask(n)
        codeBuffer = k and bMask.inv()
        k = k and bMask
        codeBits -= n
        println("d$k")
        // println("got bits $k")
        return k
    }

    private fun decodeBlockProgDc(input: InputStream, data: ShortArray, dataOffset: Int, h: Huffman, b: Int) {
        // println("dbp-dc, $codeBits, $codeBuffer")
        if (specEnd != 0) throw IOException("Can't merge dc and ac")
        if (codeBits < 16) growBufferUnsafe(input)
        if (succHigh == 0) {
            data.fill(0, dataOffset, dataOffset + 64)
            val t = h.decode(this, input)
            if (t > 15) throw IOException("Can't merge dc and ac")
            val diff = if (t > 0) extendReceive(input, t) else 0
            val comp = imgComp[b]
            val dc = comp.dcPred + diff
            comp.dcPred = dc
            data[dataOffset] = (dc * (1 shl succLow)).toShort()
        } else {
            data[dataOffset] = (data[dataOffset] + getBit(input).toInt(1 shl succLow)).toShort()
        }
        // println("data[$dataOffset] = ${data[dataOffset]}")
    }

    private fun decodeBlockProgAc(
        input: InputStream,
        data: ShortArray,
        dataOffset: Int,
        hac: Huffman,
        fac: ShortArray
    ) {
        // println("dbp-ac")
        if (specStart == 0) throw IOException("can't merge DC and AC")
        if (succHigh == 0) {
            val shift = succLow
            if (eobRun != 0) {
                eobRun--
                return
            }
            var k = specStart
            do {
                if (codeBits < 16) growBufferUnsafe(input)
                val c = (codeBuffer ushr (32 - FAST_BITS)) and FAST_MASK
                var r = fac[c].toInt()
                // println("ac $k/$specEnd $c $r, $codeBuffer/$codeBits")
                if (r != 0) {
                    k += (r shr 4) and 15
                    val s = r and 15
                    codeBuffer = codeBuffer shl s
                    codeBits -= s
                    val zig = zigzag[k++]
                    // println("data1[$zig] = ${((r shr 8) shl shift)}")
                    data[dataOffset + zig.toInt()] = ((r shr 8) shl shift).toShort()
                } else {
                    val rs = hac.decode(this, input)
                    val s = rs and 15
                    r = rs shr 4
                    // println("ac/e $k $rs $s $r")
                    if (s == 0) {
                        if (r < 15) {
                            eobRun = 1 shl r
                            if (r != 0) eobRun += getBits(input, r)
                            eobRun--
                            break
                        }
                        k += 16
                    } else {
                        k += r
                        val zig = zigzag[k++]
                        val v = (extendReceive(input, s) shl shift).toShort()
                        // println("data2[$zig] = $v")
                        data[dataOffset + zig.toInt()] = v
                    }
                    // println(" -> $eobRun, $k")
                }
            } while (k <= specEnd)
            // println("finished ac/e run")
        } else {
            val bit = (1 shl succLow).toShort()
            // println("ach $bit")
            if (eobRun != 0) {
                eobRun--
                for (k in specEnd..specEnd) {
                    val i = dataOffset + zigzag[k].toInt()
                    val p = data[i]
                    val pi = p.toInt()
                    if (pi != 0) {
                        if (getBit(input)) {
                            data[i] = (pi + sign(pi) * bit).toShort()
                        }
                    }
                }
            } else {
                var k = specStart
                do {
                    val rs = hac.decode(this, input)
                    var s = rs and 15
                    var r = rs shr 4
                    println(" ac1 $k/$specEnd $s $r $eobRun")
                    if (s == 0) {
                        if (r < 15) {
                            eobRun = (1 shl r) - 1
                            if (r != 0) {
                                eobRun += getBits(input, r)
                            }
                            r = 64
                        } else {
                            // nothing
                        }
                    } else {
                        if (s != 1) throw IOException("bad huffman code")
                        s = if (getBit(input)) +bit else -bit
                    }
                    while (k <= specEnd) {
                        val p = dataOffset + zigzag[k++].toInt()
                        val dp = data[p]
                        println(" ac2 ${k - 1}/$specEnd $dp by ${zigzag[k - 1]}")
                        if (dp != 0.toShort()) {
                            if (getBit(input)) {
                                val dpi = dp.toInt()
                                if (dpi and bit.toInt() == 0) {
                                    data[p] = (dpi + sign(dpi) * bit).toShort()
                                }
                            }
                        } else {
                            if (r == 0) {
                                data[p] = s.toShort()
                                break
                            }
                            r--
                        }
                    }
                } while (k <= specEnd)
            }
        }
    }

    fun sign(i: Int) = 1 - (i ushr 30).and(2)

    private fun leftRot(x: Int, y: Int): Int {
        return ((x shl y) or (x ushr (32 - y)))
    }

    private fun extendReceive(input: InputStream, n: Int): Int {
        if (codeBits < n) growBufferUnsafe(input)
        val sgn = codeBuffer ushr 31
        val k0 = codeBuffer
        val k = leftRot(k0, n)
        val bMask = bMask(n)
        codeBuffer = k and bMask.inv()
        val k2 = k and bMask
        codeBits -= n
        return k2 + (jBias(n) and (sgn - 1))
    }

    private fun decodeBlock(
        input: InputStream, data: ShortArray, hdc: Huffman, hac: Huffman,
        fac: ShortArray, b: Int, deq: IntArray
    ) {
        // println("db")
        if (codeBits < 16) growBufferUnsafe(input)
        val t = hdc.decode(this, input)
        if (t > 15) throw IOException("bad huffman code")
        data.fill(0)
        val diff = if (t != 0) extendReceive(input, t) else 0
        val comp = imgComp[b]
        comp.dcPred += diff
        data[0] = (comp.dcPred * deq[0]).toShort()
        // println("zero: ${comp.dcPred} + $diff, $dc * ${deq[0]} = ${data[0]}")
        var k = 1
        do {
            if (codeBits < 16) growBufferUnsafe(input)
            val c = (codeBuffer ushr (32 - FAST_BITS)) and FAST_MASK
            val r = fac[c].toInt()
            if (r != 0) {
                k += (r shr 4) and 15
                val s = r and 15
                codeBuffer = codeBuffer shl s
                codeBits -= s
                val zig = zigzag[k++].toInt()
                data[zig] = ((r shr 8) * deq[zig]).toShort()
            } else {
                val rs = hac.decode(this, input)
                val s = rs and 15
                if (s == 0) {
                    if (rs != 0xf0) break // end block
                    k += 16
                } else {
                    k += rs shr 4
                    val zig = zigzag[k++].toInt()
                    data[zig] = (extendReceive(input, s) * deq[zig]).toShort()
                }
            }
        } while (k < 64)
    }

    private fun inverseDiscreteCosineTransform(
        out: ByteArray,
        outOffset: Int,
        outStride: Int,
        data: ShortArray,
        dataOffset: Int
    ) {

        val value = idct1
        for (i in 0 until 8) {
            val j = i + dataOffset

            val s0 = data[j].toInt()
            val s1 = data[j + 8].toInt()
            val s2 = data[j + 16].toInt()
            val s3 = data[j + 24].toInt()
            val s4 = data[j + 32].toInt()
            val s5 = data[j + 40].toInt()
            val s6 = data[j + 48].toInt()
            val s7 = data[j + 56].toInt()

            if (
                s1 == 0 && s2 == 0 && s3 == 0 && s4 == 0 &&
                s5 == 0 && s6 == 0 && s7 == 0
            ) {// constant color
                val dcTerm = data[j] * 4
                for (v in 0 until 64 step 8) {
                    value[v + i] = dcTerm
                }
            } else {

                val p1 = (s2 + s6) * k0
                var t2 = p1 + s6 * k1
                var t3 = p1 + s2 * k2
                var t0 = fsh(s0 + s4)
                var t1 = fsh(s0 - s4)
                var x0 = t0 + t3
                var x3 = t0 - t3
                var x1 = t1 + t2
                var x2 = t1 - t2
                val p3 = s7 + s3
                var p4 = s5 + s1
                val p1i = s7 + s1
                var p2 = s5 + s3
                val p5 = (p3 + p4) * k3
                t0 = s7 * k4
                t1 = s5 * k5
                t2 = s3 * k6
                t3 = s1 * k7
                val p1j = p5 + p1i * k8
                p2 = p5 + p2 * k9
                val p3j = p3 * k10
                p4 *= k11
                t3 += p1j + p4
                t2 += p2 + p3j
                t1 += p2 + p4
                t0 += p1j + p3j

                x0 += 512
                x1 += 512
                x2 += 512
                x3 += 512

                value[i] = (x0 + t3) shr 10
                value[i + 8] = (x1 + t2) shr 10
                value[i + 16] = (x2 + t1) shr 10
                value[i + 24] = (x3 + t0) shr 10
                value[i + 32] = (x3 - t0) shr 10
                value[i + 40] = (x2 - t1) shr 10
                value[i + 48] = (x1 - t2) shr 10
                value[i + 56] = (x0 - t3) shr 10

            }
        }

        var vOffset = 0
        var oOffset = outOffset
        val outStride2 = outStride - 8
        val half = 65536 + (128 shl 17)
        for (i in 0 until 8) {

            val s0 = value[vOffset++]
            val s1 = value[vOffset++]
            val s2 = value[vOffset++]
            val s3 = value[vOffset++]
            val s4 = value[vOffset++]
            val s5 = value[vOffset++]
            val s6 = value[vOffset++]
            val s7 = value[vOffset++]

            val p1 = (s2 + s6) * k0
            var t2 = p1 + s6 * k1
            var t3 = p1 + s2 * k2
            var t0 = fsh(s0 + s4)
            var t1 = fsh(s0 - s4)
            var x0 = t0 + t3
            var x3 = t0 - t3
            var x1 = t1 + t2
            var x2 = t1 - t2
            val p3 = s7 + s3
            var p4 = s5 + s1
            val p1i = s7 + s1
            var p2 = s5 + s3
            val p5 = (p3 + p4) * k3
            t0 = s7 * k4
            t1 = s5 * k5
            t2 = s3 * k6
            t3 = s1 * k7
            val p1j = p5 + p1i * k8
            p2 = p5 + p2 * k9
            val p3j = p3 * k10
            p4 *= k11
            t3 += p1j + p4
            t2 += p2 + p3j
            t1 += p2 + p4
            t0 += p1j + p3j

            x0 += half
            x1 += half
            x2 += half
            x3 += half

            out[oOffset++] = clamp((x0 + t3) shr 17)
            out[oOffset++] = clamp((x1 + t2) shr 17)
            out[oOffset++] = clamp((x2 + t1) shr 17)
            out[oOffset++] = clamp((x3 + t0) shr 17)
            out[oOffset++] = clamp((x3 - t0) shr 17)
            out[oOffset++] = clamp((x2 - t1) shr 17)
            out[oOffset++] = clamp((x1 - t2) shr 17)
            out[oOffset++] = clamp((x0 - t3) shr 17)

            oOffset += outStride2

        }
    }

    // todo functions to load a layer in 8th, 4th and half size
    // (saving memory)

    private fun find8x8(data: Short) = clamp((data + 1028) shr 3)

}