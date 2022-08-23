package me.anno.tests.image.jpg

import me.anno.image.jpg.JPGThumbnails.Companion.u

fun interface ResampleFunc {
    fun run(
        out: ByteArray, line: ByteArray,
        inNear: Int, inFar: Int,
        w: Int, hs: Int
    ): Int
}

class Resample {
    var line0 = 0
    var line1 = 0
    var hs = 0
    var vs = 0
    var w = 0
    var yStep = 0
    var yPos = 0
    lateinit var data: ByteArray
    lateinit var resample: ResampleFunc
}

object ResampleRow1 : ResampleFunc {
    override fun run(out: ByteArray, line: ByteArray, inNear: Int, inFar: Int, w: Int, hs: Int): Int {
        return inNear
    }
}

object ResampleRowV2 : ResampleFunc {
    override fun run(out: ByteArray, line: ByteArray, inNear: Int, inFar: Int, w: Int, hs: Int): Int {
        for (i in 0 until w) {
            out[i] = ((3 * line[inNear + i].u() +
                    line[inFar + i].u()) shr 2).toByte()
        }
        return -1
    }
}

object ResampleRowH2 : ResampleFunc {
    override fun run(out: ByteArray, line: ByteArray, inNear: Int, inFar: Int, w: Int, hs: Int): Int {
        if (w == 1) {
            out[0] = line[inNear]
            out[1] = line[inNear]
        } else {
            out[0] = line[inNear]
            out[1] = ((line[inNear].u() * 3 + line[inNear + 1].u() + 2) shr 2).toByte()
            var i = 1
            val wm1 = w - 1
            while (i < wm1) {
                val n = 3 * line[inNear + i].u() + 2
                out[i * 2] = ((n + line[inNear + i - 1].u()) shr 2).toByte()
                out[i * 2 + 1] = ((n + line[inNear + i + 1].u()) shr 2).toByte()
                i++
            }
            out[i * 2] = ((line[inNear + w - 2].u() * 3 + line[inNear + w - 1].u() + 2) shr 2).toByte()
            out[i * 2 + 1] = line[inNear + w - 1]
        }
        return -1
    }
}

object ResampleHv2 : ResampleFunc {
    override fun run(out: ByteArray, line: ByteArray, inNear: Int, inFar: Int, w: Int, hs: Int): Int {
        if (w == 1) {
            out[0] = ((3 * line[inNear].u() + line[inFar].u() + 2) shr 2).toByte()
            out[1] = out[0]
        } else {
            // skip interpolation if out of bounds
            if (inFar >= line.size) {
                return ResampleRowGeneric.run(out, line, inNear, inFar, w, hs)
            }
            var t1 = line[inNear].u() * 3 + line[inFar].u()
            out[0] = ((t1 + 2) shr 2).toByte()
            for (i in 1 until w) {
                val t0 = t1
                t1 = 3 * line[inNear + i].u() + line[inFar + i].u()
                out[i * 2 - 1] = ((3 * t0 + t1 + 8) shr 4).toByte()
                out[i * 2] = ((3 * t1 + t0 + 8) shr 4).toByte()
            }
            out[w * 2 - 1] = ((t1 + 2) shr 2).toByte()
        }
        return -1
    }
}

object ResampleRowGeneric : ResampleFunc {
    override fun run(out: ByteArray, line: ByteArray, inNear: Int, inFar: Int, w: Int, hs: Int): Int {
        for (i in 0 until w) {
            for (j in 0 until hs) {
                out[i * hs + j] = line[inNear + i]
            }
        }
        return -1
    }
}