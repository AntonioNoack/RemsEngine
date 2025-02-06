/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.dynamic.io

import org.joml.Vector3f
import org.recast4j.dynamic.io.ByteUtils.getIntBE
import org.recast4j.dynamic.io.ByteUtils.getIntLE
import org.recast4j.dynamic.io.ByteUtils.getShortBE
import org.recast4j.dynamic.io.ByteUtils.getShortLE
import org.recast4j.dynamic.io.ByteUtils.putInt
import org.recast4j.dynamic.io.ByteUtils.putShort
import org.recast4j.recast.Heightfield
import org.recast4j.recast.Span
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoxelTile {

    val tileX: Int
    val tileZ: Int
    val borderSize: Int
    var width: Int
    var depth: Int
    val boundsMin: Vector3f
    var boundsMax: Vector3f
    var cellSize: Float
    var cellHeight: Float
    val spanData: ByteArray

    constructor(
        tileX: Int, tileZ: Int, width: Int, depth: Int, boundsMin: Vector3f, boundsMax: Vector3f, cellSize: Float,
        cellHeight: Float, borderSize: Int, buffer: ByteBuffer
    ) {
        this.tileX = tileX
        this.tileZ = tileZ
        this.width = width
        this.depth = depth
        this.boundsMin = boundsMin
        this.boundsMax = boundsMax
        this.cellSize = cellSize
        this.cellHeight = cellHeight
        this.borderSize = borderSize
        spanData = toByteArray(buffer, width, depth, VoxelFile.PREFERRED_BYTE_ORDER)
    }

    constructor(tileX: Int, tileZ: Int, heightfield: Heightfield) {
        this.tileX = tileX
        this.tileZ = tileZ
        width = heightfield.width
        depth = heightfield.height
        boundsMin = heightfield.bmin
        boundsMax = heightfield.bmax
        cellSize = heightfield.cellSize
        cellHeight = heightfield.cellHeight
        borderSize = heightfield.borderSize
        spanData = serializeSpans(heightfield, VoxelFile.PREFERRED_BYTE_ORDER)
    }

    fun heightfield(): Heightfield {
        return if (VoxelFile.PREFERRED_BYTE_ORDER == ByteOrder.BIG_ENDIAN) heightfieldBE() else heightfieldLE()
    }

    private fun heightfieldBE(): Heightfield {
        val hf = Heightfield(width, depth, boundsMin, boundsMax, cellSize, cellHeight, borderSize)
        var position = 0
        var z = 0
        var pz = 0
        while (z < depth) {
            for (x in 0 until width) {
                var prev: Span? = null
                val spanCount: Int = getShortBE(spanData, position)
                position += 2
                for (s in 0 until spanCount) {
                    val span = Span()
                    span.min = getIntBE(spanData, position)
                    position += 4
                    span.max = getIntBE(spanData, position)
                    position += 4
                    span.area = getIntBE(spanData, position)
                    position += 4
                    if (prev == null) {
                        hf.spans[pz + x] = span
                    } else {
                        prev.next = span
                    }
                    prev = span
                }
            }
            z++
            pz += width
        }
        return hf
    }

    private fun heightfieldLE(): Heightfield {
        val hf = Heightfield(width, depth, boundsMin, boundsMax, cellSize, cellHeight, borderSize)
        var position = 0
        var z = 0
        var pz = 0
        while (z < depth) {
            for (x in 0 until width) {
                var prev: Span? = null
                val spanCount: Int = getShortLE(spanData, position)
                position += 2
                for (s in 0 until spanCount) {
                    val span = Span()
                    span.min = getIntLE(spanData, position)
                    position += 4
                    span.max = getIntLE(spanData, position)
                    position += 4
                    span.area = getIntLE(spanData, position)
                    position += 4
                    if (prev == null) {
                        hf.spans[pz + x] = span
                    } else {
                        prev.next = span
                    }
                    prev = span
                }
            }
            z++
            pz += width
        }
        return hf
    }

    private fun serializeSpans(heightfield: Heightfield, order: ByteOrder): ByteArray {
        val counts = IntArray(heightfield.width * heightfield.height)
        var totalCount = serializeSpans0(heightfield, counts)
        return serializeSpans1(heightfield, order, counts, totalCount)
    }

    private fun serializeSpans0(heightfield: Heightfield, counts: IntArray): Int {
        var totalCount = 0
        var pz = 0
        for (z in 0 until heightfield.height) {
            for (x in 0 until heightfield.width) {
                var span = heightfield.spans[pz + x]
                while (span != null) {
                    counts[pz + x]++
                    totalCount++
                    span = span.next
                }
            }
            pz += heightfield.width
        }
        return totalCount
    }

    private fun serializeSpans1(
        heightfield: Heightfield, order: ByteOrder,
        counts: IntArray, totalCount: Int
    ): ByteArray {
        var position = 0
        var pz = 0
        val data = ByteArray(totalCount * SERIALIZED_SPAN_BYTES + counts.size * SERIALIZED_SPAN_COUNT_BYTES)
        for (z in 0 until heightfield.height) {
            for (x in 0 until heightfield.width) {
                position = putShort(counts[pz + x], data, position, order)
                var span = heightfield.spans[pz + x]
                while (span != null) {
                    position = putInt(span.min, data, position, order)
                    position = putInt(span.max, data, position, order)
                    position = putInt(span.area, data, position, order)
                    span = span.next
                }
            }
            pz += heightfield.width
        }
        return data
    }

    private fun toByteArray(buf: ByteBuffer, width: Int, height: Int, order: ByteOrder): ByteArray {
        val data = ByteArray(buf.limit())
        if (buf.order() == order) {
            buf.get(data)
        } else {
            val l = Math.multiplyExact(width, height)
            var position = 0
            for (i in 0 until l) {
                val count = buf.getShort().toInt()
                putShort(count, data, position, order)
                position += 2
                for (j in 0 until count) {
                    putInt(buf.getInt(), data, position, order)
                    putInt(buf.getInt(), data, position + 4, order)
                    putInt(buf.getInt(), data, position + 8, order)
                    position += 12
                }
            }
        }
        return data
    }

    companion object {
        private const val SERIALIZED_SPAN_COUNT_BYTES = 2
        private const val SERIALIZED_SPAN_BYTES = 12
    }
}