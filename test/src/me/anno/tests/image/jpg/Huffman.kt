package me.anno.tests.image.jpg

import me.anno.image.jpg.JPGThumbnails
import me.anno.image.jpg.JPGThumbnails.Companion.FAST_BITS
import me.anno.image.jpg.JPGThumbnails.Companion.FAST_MASK
import me.anno.image.jpg.JPGThumbnails.Companion.FAST_SIZE
import me.anno.image.jpg.JPGThumbnails.Companion.bMask
import me.anno.image.jpg.JPGThumbnails.Companion.u
import java.io.IOException
import java.io.InputStream

class Huffman {

    val fast = ByteArray(FAST_SIZE)
    val values = ByteArray(256)
    val size = ByteArray(257)

    private val code = ShortArray(256)
    private val maxCode = IntArray(17)
    private val delta = IntArray(17)

    var ctr = 0

    fun build(count: IntArray) {
        println("--- huffman table:        ${count.joinToString()}, ")
        var k = 0
        for (i in 0 until 16) {
            val v = (i + 1).toByte()
            size.fill(v, k, k + count[i])
            k += count[i]
        }
        size[k] = 0
        k = 0
        var code = 0
        for (j in 1..16) {
            delta[j] = k - code
            if (size[k].toInt() == j) {
                while (size[k].toInt() == j) {
                    this.code[k++] = (code++).toShort()
                }
                if (code - 1 >= 1 shl j) throw IOException("bad code lengths")
            }
            maxCode[j] = code shl (16 - j)
            code = code shl 1
        }
        fast.fill(-1)
        for (i in 0 until k) {
            val s = size[i]
            if (s <= FAST_BITS) {
                val shift = FAST_BITS - s
                val c = (this.code[i].toInt() and 0xffff) shl shift
                fast.fill(i.toByte(), c, c + (1 shl shift))
            }
        }
        // looks completely correct
        println("fast codes:               ${fast.joinToString { it.toUByte().toString() }}, ")
        println("max codes:                ${maxCode.joinToString()}, -1, ")
        println("deltas:                   ${delta.joinToString()}, ")
    }

    fun decode(img: JPGThumbnails, input: InputStream): Int {
        if (img.codeBits < 16) img.growBufferUnsafe(input)
        var c = (img.codeBuffer shr (32 - FAST_BITS)) and FAST_MASK
        var k = fast[c].u()
        println("$c/$k")
        if (k < 255) {
            val s = size[k].toInt()
            if (s > img.codeBits)
                throw IOException("bad huffman code")
            img.codeBuffer = img.codeBuffer shl s
            img.codeBits -= s
            // println("   ${ctr++} $s $k: ${values[k].u()}, [${img.codeBuffer}/${img.codeBits}]")
            return values[k].u()
        } else {
            val temp = img.codeBuffer ushr 16
            k = FAST_BITS + 1
            while (k < 17 && temp >= maxCode[k]) {
                k++
            }
            if (k == 17) {
                var rem = 0
                while (true){
                    if(input.read() < 0) break
                    rem++
                }
                throw IOException("bad huffman code, $temp, ${maxCode.joinToString()}, rem: $rem")
            }
            if (k > img.codeBits) throw IOException("bad huffman code")
            c = ((img.codeBuffer ushr (32 - k)) and bMask(k)) + delta[k]
            // println("--- ${ctr++} $c: ${values[c].u()} by $temp,$k")
            img.codeBits -= k
            img.codeBuffer = img.codeBuffer shl k
            return values[c].u()
        }
    }
}