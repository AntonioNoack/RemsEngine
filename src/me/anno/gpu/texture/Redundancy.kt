package me.anno.gpu.texture

import me.anno.utils.algorithms.ForLoop.forLoop
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

object Redundancy {

    fun Texture2D.checkRedundancyX4(data: IntArray): IntArray {
        if (width * height <= 1) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return intArrayOf(c0)
    }

    fun Texture2D.checkRedundancyX4(data: IntBuffer) {
        if (width * height <= 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun Texture2D.checkRedundancyX1(data: ByteBuffer) {
        if (data.capacity() <= 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun Texture2D.checkRedundancyX1(data: FloatArray): FloatArray {
        if (data.isEmpty()) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return floatArrayOf(data[0])
    }

    fun Texture2D.checkRedundancyX1(data: FloatBuffer) {
        if (data.capacity() < 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun Texture2D.checkRedundancyX1(data: ShortBuffer) {
        if (data.capacity() < 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun Texture2D.checkRedundancyX1(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return byteArrayOf(c0)
    }

    fun Texture2D.checkRedundancyX4(data: FloatArray): FloatArray {
        if (data.size < 4) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        forLoop(4, width * height * 4, 4) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2, c3)
    }

    fun Texture2D.checkRedundancyX3(data: FloatArray): FloatArray {
        if (data.size < 3) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        forLoop(3, width * height * 3, 3) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2)
    }

    fun Texture2D.checkRedundancyX3(data: FloatBuffer) {
        if (data.capacity() < 3) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        forLoop(3, width * height * 3, 3) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
        }
        setSize1x1()
        data.limit(3)
    }

    fun Texture2D.checkRedundancyX4(data: FloatBuffer) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        forLoop(4, width * height * 4, 4) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    fun Texture2D.checkRedundancyX4(data: ByteArray): ByteArray {
        if (data.size < 4) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        forLoop(4, width * height * 4, 4) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2, c3)
    }

    fun Texture2D.checkRedundancyX4(data: ByteBuffer) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        forLoop(4, width * height * 4, 4) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    fun Texture2D.checkRedundancyX2(data: ByteArray): ByteArray {
        if (data.size < 2) return data
        val c0 = data[0]
        val c1 = data[1]
        forLoop(2, width * height * 2, 2) { i ->
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1)
    }

    fun Texture2D.checkRedundancyX2(data: FloatArray): FloatArray {
        if (data.size < 2) return data
        val c0 = data[0]
        val c1 = data[1]
        forLoop(2, width * height * 2, 2) { i ->
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1)
    }

    fun Texture2D.checkRedundancyX3(data: ByteArray): ByteArray {
        if (data.size < 3) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        forLoop(3, width * height * 3, 3) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2)
    }

    fun Texture2D.checkRedundancyX3(data: ByteBuffer) {
        if (data.capacity() < 3) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        forLoop(3, width * height * 3, 3) { i ->
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return
        }
        setSize1x1()
        data.limit(3)
    }

    fun Texture2D.checkRedundancyX4(data: ByteBuffer, rgbOnly: Boolean) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        if (rgbOnly) {
            forLoop(4, width * height * 4, 4) { i ->
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
            }
        } else {
            forLoop(4, width * height * 4, 4) { i ->
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
            }
        }
        setSize1x1()
        data.limit(4)
    }

    fun Texture2D.checkRedundancyX2(data: ByteBuffer) {
        // when rgbOnly, check rgb only?
        if (data.capacity() < 2) return
        val c0 = data[0]
        val c1 = data[1]
        forLoop(2, width * height * 2, 2) { i ->
            if (c0 != data[i] || c1 != data[i + 1]) return
        }
        setSize1x1()
        data.limit(2)
    }
}