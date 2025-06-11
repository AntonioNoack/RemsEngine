package com.bulletphysics.collision.shapes

import java.io.Serializable
import kotlin.math.min

/**
 * QuantizedBvhNodes is array of compressed AABB nodes, each of 16 bytes.
 * Node can be used for leaf node or internal node. Leaf nodes can point to 32-bit
 * triangle index (non-negative range).
 *
 *
 *
 * *Implementation note:* the nodes are internally stored in int[] array
 * and bit packed. The actual structure is:
 *
 * <pre>
 * unsigned short  quantizedAabbMin[3]
 * unsigned short  quantizedAabbMax[3]
 * signed   int    escapeIndexOrTriangleIndex
</pre> *
 *
 * @author jezek2
 */
class QuantizedBvhNodes : Serializable {

    private var buf: IntArray = IntArray(16 * STRIDE)
    private var size = 0

    fun add(): Int {
        while (size + 1 >= capacity()) {
            resize(capacity() * 2)
        }
        return size++
    }

    fun size(): Int {
        return size
    }

    fun capacity(): Int {
        return buf.size / STRIDE
    }

    fun clear() {
        size = 0
    }

    fun resize(num: Int) {
        val oldBuf = buf
        buf = IntArray(num * STRIDE)
        System.arraycopy(oldBuf, 0, buf, 0, min(oldBuf.size, buf.size))
    }

    fun set(destId: Int, srcNodes: QuantizedBvhNodes, srcId: Int) {
        // save field access:

        val buf = this.buf
        val srcBuf = srcNodes.buf

        buf[destId * STRIDE] = srcBuf[srcId * STRIDE]
        buf[destId * STRIDE + 1] = srcBuf[srcId * STRIDE + 1]
        buf[destId * STRIDE + 2] = srcBuf[srcId * STRIDE + 2]
        buf[destId * STRIDE + 3] = srcBuf[srcId * STRIDE + 3]
    }

    fun swap(id1: Int, id2: Int) {
        // save field access:

        val buf = this.buf

        val temp0 = buf[id1 * STRIDE]
        val temp1 = buf[id1 * STRIDE + 1]
        val temp2 = buf[id1 * STRIDE + 2]
        val temp3 = buf[id1 * STRIDE + 3]

        buf[id1 * STRIDE] = buf[id2 * STRIDE]
        buf[id1 * STRIDE + 1] = buf[id2 * STRIDE + 1]
        buf[id1 * STRIDE + 2] = buf[id2 * STRIDE + 2]
        buf[id1 * STRIDE + 3] = buf[id2 * STRIDE + 3]

        buf[id2 * STRIDE] = temp0
        buf[id2 * STRIDE + 1] = temp1
        buf[id2 * STRIDE + 2] = temp2
        buf[id2 * STRIDE + 3] = temp3
    }

    fun getQuantizedAabbMin(nodeId: Int, index: Int): Int {
        return when (index) {
            1 -> buf[nodeId * STRIDE] ushr 16
            2 -> buf[nodeId * STRIDE + 1]
            else -> buf[nodeId * STRIDE]
        } and 0xFFFF
    }

    fun getQuantizedAabbMin(nodeId: Int): Long {
        return (buf[nodeId * STRIDE].toLong() and 0xFFFFFFFFL) or ((buf[nodeId * STRIDE + 1].toLong() and 0xFFFFL) shl 32)
    }

    fun setQuantizedAabbMin(nodeId: Int, value: Long) {
        buf[nodeId * STRIDE] = value.toInt()
        setQuantizedAabbMin(nodeId, 2, ((value and 0xFFFF00000000L) ushr 32).toShort().toInt())
    }

    fun setQuantizedAabbMax(nodeId: Int, value: Long) {
        setQuantizedAabbMax(nodeId, 0, value.toShort().toInt())
        buf[nodeId * STRIDE + 2] = (value ushr 16).toInt()
    }

    fun setQuantizedAabbMin(nodeId: Int, index: Int, value: Int) {
        when (index) {
            0 -> buf[nodeId * STRIDE] = (buf[nodeId * STRIDE] and -0x10000) or (value and 0xFFFF)
            1 -> buf[nodeId * STRIDE] = (buf[nodeId * STRIDE] and 0x0000FFFF) or ((value and 0xFFFF) shl 16)
            2 -> buf[nodeId * STRIDE + 1] = (buf[nodeId * STRIDE + 1] and -0x10000) or (value and 0xFFFF)
        }
    }

    fun getQuantizedAabbMax(nodeId: Int, index: Int): Int {
        when (index) {
            1 -> return (buf[nodeId * STRIDE + 2]) and 0xFFFF
            2 -> return (buf[nodeId * STRIDE + 2] ushr 16) and 0xFFFF
            else -> return (buf[nodeId * STRIDE + 1] ushr 16) and 0xFFFF
        }
    }

    fun getQuantizedAabbMax(nodeId: Int): Long {
        return ((buf[nodeId * STRIDE + 1].toLong() and 0xFFFF0000L) ushr 16) or ((buf[nodeId * STRIDE + 2].toLong() and 0xFFFFFFFFL) shl 16)
    }

    fun setQuantizedAabbMax(nodeId: Int, index: Int, value: Int) {
        when (index) {
            0 -> buf[nodeId * STRIDE + 1] = (buf[nodeId * STRIDE + 1] and 0x0000FFFF) or ((value and 0xFFFF) shl 16)
            1 -> buf[nodeId * STRIDE + 2] = (buf[nodeId * STRIDE + 2] and -0x10000) or (value and 0xFFFF)
            2 -> buf[nodeId * STRIDE + 2] = (buf[nodeId * STRIDE + 2] and 0x0000FFFF) or ((value and 0xFFFF) shl 16)
        }
    }

    fun getEscapeIndexOrTriangleIndex(nodeId: Int): Int {
        return buf[nodeId * STRIDE + 3]
    }

    fun setEscapeIndexOrTriangleIndex(nodeId: Int, value: Int) {
        buf[nodeId * STRIDE + 3] = value
    }

    fun isLeafNode(nodeId: Int): Boolean {
        // skipindex is negative (internal node), triangleindex >=0 (leafnode)
        return (getEscapeIndexOrTriangleIndex(nodeId) >= 0)
    }

    fun getEscapeIndex(nodeId: Int): Int {
        assert(!isLeafNode(nodeId))
        return -getEscapeIndexOrTriangleIndex(nodeId)
    }

    fun getTriangleIndex(nodeId: Int): Int {
        assert(isLeafNode(nodeId))
        // Get only the lower bits where the triangle index is stored
        return (getEscapeIndexOrTriangleIndex(nodeId) and ((0.inv()) shl (31 - OptimizedBvh.MAX_NUM_PARTS_IN_BITS)).inv())
    }

    fun getPartId(nodeId: Int): Int {
        assert(isLeafNode(nodeId))
        // Get only the highest bits where the part index is stored
        return (getEscapeIndexOrTriangleIndex(nodeId) ushr (31 - OptimizedBvh.MAX_NUM_PARTS_IN_BITS))
    }

    companion object {
        private const val STRIDE = 4 // 16 bytes

        @JvmStatic
        val nodeSize: Int
            get() = STRIDE * 4

        @JvmStatic
        fun getCoord(vec: Long, index: Int): Int {
            val shift = index * 16
            return (vec ushr shift).toInt() and 0xFFFF
        }
    }
}
