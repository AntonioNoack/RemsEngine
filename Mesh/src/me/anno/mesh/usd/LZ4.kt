package me.anno.mesh.usd

/**
 * Implementation from TinyUSDZ, converted using ChatGPT
 * */
object LZ4 {

    const val LZ4_MAX_INPUT_SIZE = 0x7E000000

    // -----------------------------
    // Lightweight input reader
    // -----------------------------
    private class ByteArrayInput(
        private val data: ByteArray,
        private val limit: Int
    ) {
        var pos = 0

        fun remaining(): Int = limit - pos

        fun readByte(): Int {
            if (pos >= limit) error("EOF")
            return data[pos++].toInt() and 0xFF
        }

        fun readIntLE(): Int {
            if (pos + 4 > limit) error("EOF")
            val v =
                (data[pos].toInt() and 0xFF) or
                        ((data[pos + 1].toInt() and 0xFF) shl 8) or
                        ((data[pos + 2].toInt() and 0xFF) shl 16) or
                        ((data[pos + 3].toInt() and 0xFF) shl 24)
            pos += 4
            return v
        }
    }

    // -----------------------------
    // Public API (container-aware)
    // -----------------------------
    fun decompressFromBuffer(
        src: ByteArray,
        srcSize: Int,
        dst: ByteArray,
        maxOutputSize: Int,
        err: StringBuilder? = null
    ): Int {

        if (srcSize <= 1) {
            err?.append("Invalid compressedSize.\n")
            return 0
        }

        val input = ByteArrayInput(src, srcSize)

        val nChunks = input.readByte()
        if (nChunks > 127) {
            err?.append("Too many chunks in LZ4 compressed data.\n")
            return 0
        }

        if (maxOutputSize < LZ4_MAX_INPUT_SIZE && nChunks != 0) {
            err?.append("Corrupted LZ4 compressed data.\n")
            return 0
        }

        var outputPos = 0

        // ---- single chunk fast path ----
        if (nChunks == 0) {
            val decoded = decompressSafe(
                src, input.pos,
                dst, outputPos,
                srcSize - 1,
                maxOutputSize
            )

            if (decoded < 0) {
                err?.append("Failed to decompress data, LZ4 error: $decoded\n")
                return 0
            }
            return decoded
        }

        // ---- multi-chunk ----
        var totalDecompressed = 0

        repeat(nChunks) { chunkIndex ->

            if (input.remaining() < 4) {
                err?.append("Corrupted chunk data.")
                return 0
            }

            val chunkSize = input.readIntLE()

            if (chunkSize <= 0) {
                err?.append("Invalid ChunkSize.\n")
                return 0
            }

            if (chunkSize > LZ4_MAX_INPUT_SIZE) {
                err?.append("ChunkSize exceeds LZ4_MAX_INPUT_SIZE.\n")
                return 0
            }

            if (input.remaining() < chunkSize) {
                err?.append("Total chunk size exceeds input compressedSize.\n")
                return 0
            }

            val decoded = decompressSafe(
                src, input.pos,
                dst, outputPos,
                chunkSize,
                minOf(LZ4_MAX_INPUT_SIZE, maxOutputSize - outputPos)
            )

            if (decoded <= 0) {
                err?.append("Failed to decompress data, LZ4 error: $decoded\n")
                return 0
            }

            if (decoded > (maxOutputSize - outputPos)) {
                err?.append("Output buffer overflow.\n")
                return 0
            }

            input.pos += chunkSize
            outputPos += decoded
            totalDecompressed += decoded
        }

        return totalDecompressed
    }

    // -----------------------------
    // Core LZ4 safe decompressor
    // (wrapper over your generic)
    // -----------------------------
    fun decompressSafe(
        src: ByteArray, srcOff: Int,
        dst: ByteArray, dstOff: Int,
        srcSize: Int, dstCapacity: Int
    ): Int {
        return decompressGeneric(
            src, srcOff,
            dst, dstOff,
            srcSize, dstCapacity
        )
    }

    // -----------------------------
    // Core implementation (simplified full parity version)
    // -----------------------------
    private fun decompressGeneric(
        src: ByteArray,
        srcOff: Int,
        dst: ByteArray,
        dstOff: Int,
        srcSize: Int,
        dstCapacity: Int
    ): Int {

        var ip = srcOff
        val iend = srcOff + srcSize

        var op = dstOff
        val oend = dstOff + dstCapacity

        while (true) {
            if (ip >= iend) break

            val token = src[ip++].toInt() and 0xFF

            // literals
            var length = token ushr 4
            if (length == 15) {
                var s: Int
                do {
                    if (ip >= iend) return error(ip, srcOff)
                    s = src[ip++].toInt() and 0xFF
                    length += s
                } while (s == 255)
            }

            if (ip + length > iend || op + length > oend) {
                if (ip + length != iend) return error(ip, srcOff)
                System.arraycopy(src, ip, dst, op, length)
                op += length
                break
            }

            System.arraycopy(src, ip, dst, op, length)
            ip += length
            op += length

            if (ip + 1 >= iend) break

            val offset =
                (src[ip].toInt() and 0xFF) or
                        ((src[ip + 1].toInt() and 0xFF) shl 8)
            ip += 2

            val match = op - offset
            if (match < dstOff) return error(ip, srcOff)

            length = token and 0x0F
            if (length == 15) {
                var s: Int
                do {
                    if (ip >= iend) return error(ip, srcOff)
                    s = src[ip++].toInt() and 0xFF
                    length += s
                } while (s == 255)
            }
            length += 4

            val end = op + length
            if (end > oend) return error(ip, srcOff)

            if (offset < length) {
                var m = match
                while (op < end) dst[op++] = dst[m++]
            } else {
                System.arraycopy(dst, match, dst, op, length)
                op += length
            }
        }

        return op - dstOff
    }

    private fun error(ip: Int, base: Int): Int {
        return -(ip - base) - 1
    }

    private fun LZ4_compressBound(isize: Int): Int {
        if (isize > LZ4_MAX_INPUT_SIZE) return 0
        return isize + isize / 255 + 16
    }

    private fun maxInputSize(): Long = 127L * LZ4_MAX_INPUT_SIZE

    fun getMaxSize(inputSize: Int): Int {
        if (inputSize > maxInputSize()) return 0;

        // If it fits in one chunk then it's just the compress bound plus 1.
        if (inputSize <= LZ4_MAX_INPUT_SIZE) {
            return (LZ4_compressBound(inputSize)) + 1
        }

        val nWholeChunks = inputSize / LZ4_MAX_INPUT_SIZE
        val partChunkSz = inputSize % LZ4_MAX_INPUT_SIZE
        var sz = 1 + nWholeChunks * ((LZ4_compressBound(LZ4_MAX_INPUT_SIZE)) + 4)
        if (partChunkSz > 0) sz += (LZ4_compressBound(partChunkSz)) + 4
        return sz;
    }
}