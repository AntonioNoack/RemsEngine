package net.boeckling.crc

import java.io.IOException
import java.io.InputStream

/**
 * CRC-64 implementation with ability to combine checksums calculated over
 * different blocks of data.
 *
 * This is a faster version of the original implementation by R. Nikitchenko,
 * incorporating the nested lookup table design by Mark Adler (see [Stackoverflow](http://stackoverflow.com/a/20579405/58962)).
 *
 * Throughput rose from 375 MB/s to 1150 MB/s on a 2,3 GHz i7, which is 3.06
 * times faster.
 *
 * @author Roman Nikitchenko (roman@nikitchenko.dp.ua)
 * @author Michael Böckling
 */
object CRC64 {

    private const val POLY = -0x3693a86a2878f0beL // ECMA-182

    /* CRC64 calculation table. */
    private val table = Array(8) { LongArray(256) }

    /**
     * Calculate the CRC64 of the given [InputStream] until the end of the
     * stream has been reached.
     *
     * @param `in` the stream will be closed automatically
     * @return new [CRC64] instance initialized to the [InputStream]'s CRC value
     * @throws IOException in case the [InputStream.read] method fails
     */
    @Throws(IOException::class)
    fun fromInputStream(input: InputStream): Long {
        return input.use {
            var crc = 0L
            val buffer = ByteArray(65536)
            while (true) {
                val length = input.read()
                if (length == -1) break
                crc = update(buffer, length, crc)
            }
            crc
        }
    }

    // dimension of GF(2) vectors (length of CRC)
    private const val GF2_DIM = 64
    private fun gf2MatrixTimes(mat: LongArray, value0: Long): Long {
        var vec = value0
        var sum = 0L
        var idx = 0
        while (vec != 0L) {
            if (vec and 1 == 1L) sum = sum xor mat[idx]
            vec = vec ushr 1
            idx++
        }
        return sum
    }

    private fun gf2MatrixSquare(square: LongArray, mat: LongArray) {
        for (n in 0 until GF2_DIM) square[n] = gf2MatrixTimes(mat, mat[n])
    }

    /*
     * Return the CRC-64 of two sequential blocks, where summ1 is the CRC-64 of
     * the first block, summ2 is the CRC-64 of the second block, and len2 is the
     * length of the second block.
     */
    fun combine(summ1: Long, summ2: Long, len2x: Long): Long {
        // degenerate case.
        var len2 = len2x
        if (len2 == 0L) return summ1

        var row: Long
        val even = LongArray(GF2_DIM) // even-power-of-two zeros operator
        val odd = LongArray(GF2_DIM) // odd-power-of-two zeros operator

        // put operator for one zero bit in odd
        odd[0] = POLY // CRC-64 polynomial
        row = 1
        var n = 1
        while (n < GF2_DIM) {
            odd[n] = row
            row = row shl 1
            n++
        }

        // put operator for two zero bits in even
        gf2MatrixSquare(even, odd)

        // put operator for four zero bits in odd
        gf2MatrixSquare(odd, even)

        // apply len2 zeros to crc1 (first square will put the operator for one
        // zero byte, eight zero bits, in even)
        var crc1 = summ1
        val crc2 = summ2
        do {
            // apply zeros operator for this bit of len2
            gf2MatrixSquare(even, odd)
            if (len2 and 1 == 1L) crc1 = gf2MatrixTimes(even, crc1)
            len2 = len2 ushr 1

            // if no more bits set, then done
            if (len2 == 0L) break

            // another iteration of the loop with odd and even swapped
            gf2MatrixSquare(odd, even)
            if (len2 and 1 == 1L) crc1 = gf2MatrixTimes(odd, crc1)
            len2 = len2 ushr 1

            // if no more bits set, then done
        } while (len2 != 0L)

        // return combined crc.
        crc1 = crc1 xor crc2
        return crc1
    }

    init {
        /*
         * Nested tables as described by Mark Adler:
         * http://stackoverflow.com/a/20579405/58962
         */
        for (n in 0..255) {
            var crc = n.toLong()
            for (k in 0..7) {
                crc = if (crc and 1 == 1L) {
                    crc ushr 1 xor POLY
                } else {
                    crc ushr 1
                }
            }
            table[0][n] = crc
        }

        /* generate nested CRC table for future slice-by-8 lookup */
        for (n in 0..255) {
            var crc = table[0][n]
            for (k in 1..7) {
                crc =
                    table[0][(crc and 0xff).toInt()] xor (crc ushr 8)
                table[k][n] = crc
            }
        }
    }

    /**
     * Update CRC64 with new byte block.
     */
    fun update(b: ByteArray, len: Int, value0: Long): Long {
        return update(b, 0, len, value0)
    }

    /**
     * Update CRC64 with new byte block.
     */
    fun update(b: ByteArray, off: Int, len0: Int, value0: Long): Long {
        var len = len0
        var value = value0.inv()

        /* fast middle processing, 8 bytes (aligned!) per loop */
        var idx = off
        while (len >= 8) {
            value = (table[7][(value and 0xff xor (b[idx].toLong() and 0xff)).toInt()]
                    xor table[6][(value ushr 8 and 0xff xor (b[idx + 1].toLong() and 0xff)).toInt()]
                    xor table[5][(value ushr 16 and 0xff xor (b[idx + 2].toLong() and 0xff)).toInt()]
                    xor table[4][(value ushr 24 and 0xff xor (b[idx + 3].toLong() and 0xff)).toInt()]
                    xor table[3][(value ushr 32 and 0xff xor (b[idx + 4].toLong() and 0xff)).toInt()]
                    xor table[2][(value ushr 40 and 0xff xor (b[idx + 5].toLong() and 0xff)).toInt()]
                    xor table[1][(value ushr 48 and 0xff xor (b[idx + 6].toLong() and 0xff)).toInt()]
                    xor table[0][(value ushr 56 xor b[idx + 7].toLong() and 0xff).toInt()])
            idx += 8
            len -= 8
        }

        /* process remaining bytes (can't be larger than 8) */while (len > 0) {
            value = table[0][(value xor b[idx].toLong() and 0xff).toInt()] xor (value ushr 8)
            idx++
            len--
        }
        value = value.inv()
        return value
    }

    fun update(b: Int, value0: Long): Long {
        return update(byteArrayOf(b.toByte()), 0, 1, value0)
    }

    fun toString(value: Long): String {
        return "CRC64{value=" + value.toULong().toString(16) + '}'
    }
}