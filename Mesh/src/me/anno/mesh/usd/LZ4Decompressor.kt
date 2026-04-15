package me.anno.mesh.usd

object LZ4Decompressor {

    fun decompress(src: ByteArray, decompressedSize: Int): ByteArray {
        val dst = ByteArray(decompressedSize)

        var si = 0 // source index
        var di = 0 // destination index

        fun readByte(): Int = src[si++].toInt() and 0xFF

        while (si < src.size && di < decompressedSize) {

            // Token
            val token = readByte()

            // Literal length
            var literalLen = token ushr 4
            if (literalLen == 15) {
                var b: Int
                do {
                    b = readByte()
                    literalLen += b
                } while (b == 255)
            }

            // Copy literals
            for (i in 0 until literalLen) {
                dst[di++] = src[si++]
            }

            // End of stream
            if (si >= src.size || di >= decompressedSize) break

            // Match offset (2 bytes LE)
            val offset = readByte() or (readByte() shl 8)
            if (offset == 0) throw IllegalArgumentException("Invalid LZ4 offset = 0")

            // Match length
            var matchLen = (token and 0x0F) + 4
            if ((token and 0x0F) == 15) {
                var b: Int
                do {
                    b = readByte()
                    matchLen += b
                } while (b == 255)
            }

            // Copy match (may overlap → backward copy required)
            var ref = di - offset
            if (ref < 0) throw IllegalArgumentException("Invalid offset beyond buffer")

            repeat(matchLen) {
                dst[di++] = dst[ref++]
            }
        }

        return dst
    }
}