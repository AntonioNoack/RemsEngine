package net.boeckling.crc

/**
 * CRC-64 implementation with ability to combine checksums calculated over
 * different blocks of data.
 *
 * This is a faster version of the original implementation by R. Nikitchenko,
 * incorporating the nested lookup table design by Mark Adler (see [Stackoverflow](http://stackoverflow.com/a/20579405/58962)).
 *
 * Throughput rose from 375 MB/s to 1150 MB/s on a 2,3 GHz i7, which is 3.06 times faster.
 *
 * @author Roman Nikitchenko (roman@nikitchenko.dp.ua)
 * @author Michael Böckling
 */
object CRC64 {

    /** CRC64 calculation table. */
    private val table = LongArray(8 * 256)

    init {
        val poly = -0x3693a86a2878f0beL // ECMA-182
        /*
         * Nested tables as described by Mark Adler:
         * http://stackoverflow.com/a/20579405/58962
         */
        for (n in 0 until 256) {
            var crc = n.toLong()
            for (k in 0 until 8) {
                crc = if (crc.and(1L) == 1L) {
                    crc.ushr(1).xor(poly)
                } else {
                    crc.ushr(1)
                }
            }
            table[n] = crc
        }

        /* generate nested CRC table for future slice-by-8 lookup */
        for (n in 0 until 256) {
            var crc = table[n]
            for (k in 1 until 8) {
                crc = table[(crc and 0xff).toInt()] xor (crc ushr 8)
                table[k.shl(8) + n] = crc
            }
        }
    }

    /**
     * Update CRC64 with new byte block.
     */
    @JvmStatic
    fun update(b: ByteArray, offset: Int, length: Int, value0: Long): Long {
        val end = offset + length
        val endM7 = end - 7
        var value = value0.inv()

        /* fast middle processing, 8 bytes (aligned!) per loop */
        var i = offset
        while (i < endM7) {
            value = table[0x700 + getValue(value, b[i])] xor
                    table[0x600 + getValue(value ushr 8, b[i + 1])] xor
                    table[0x500 + getValue(value ushr 16, b[i + 2])] xor
                    table[0x400 + getValue(value ushr 24, b[i + 3])] xor
                    table[0x300 + getValue(value ushr 32, b[i + 4])] xor
                    table[0x200 + getValue(value ushr 40, b[i + 5])] xor
                    table[0x100 + getValue(value ushr 48, b[i + 6])] xor
                    table[0x000 + getValue(value ushr 56, b[i + 7])]
            i += 8
        }

        // process remaining bytes (can't be larger than 8)
        while (i < end) {
            value = table[getValue(value, b[i++])] xor (value ushr 8)
        }
        value = value.inv()
        return value
    }

    @JvmStatic
    private fun getValue(value: Long, bi: Byte): Int {
        return (value.toInt().xor(bi.toInt())).and(255)
    }
}