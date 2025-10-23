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

import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import org.joml.AABBf
import org.recast4j.dynamic.io.ByteUtils.putInt
import org.recast4j.dynamic.io.ByteUtils.putShort
import org.recast4j.recast.Heightfield
import org.recast4j.recast.Span
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoxelTile {

    val tileX: Int
    val tileZ: Int
    val borderSize: Int
    var width: Int
    var depth: Int
    val bounds: AABBf
    var cellSize: Float
    var cellHeight: Float
    val spanData: ByteArray

    constructor(
        tileX: Int, tileZ: Int, width: Int, depth: Int, bounds: AABBf, cellSize: Float,
        cellHeight: Float, borderSize: Int, buffer: ByteBuffer
    ) {
        this.tileX = tileX
        this.tileZ = tileZ
        this.width = width
        this.depth = depth
        this.bounds = bounds
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
        bounds = heightfield.bounds
        cellSize = heightfield.cellSize
        cellHeight = heightfield.cellHeight
        borderSize = heightfield.borderSize
        spanData = serializeSpans(heightfield, VoxelFile.PREFERRED_BYTE_ORDER)
    }

    fun heightfield(): Heightfield {
        return if (VoxelFile.PREFERRED_BYTE_ORDER == ByteOrder.BIG_ENDIAN) heightfieldBE() else heightfieldLE()
    }

    private fun heightfieldBE(): Heightfield {
        val field = Heightfield(width, depth, bounds, cellSize, cellHeight, borderSize)
        val stream = ByteArrayInputStream(spanData)
        for (fieldIndex in 0 until width * depth) {
            var prev: Span? = null
            val spanCount = stream.readBE16()
            repeat(spanCount) {
                val span = Span()
                span.min = stream.readBE32()
                span.max = stream.readBE32()
                span.area = stream.readBE32()
                if (prev == null) {
                    field.spans[fieldIndex] = span
                } else {
                    prev.next = span
                }
                prev = span
            }
        }
        return field
    }

    private fun heightfieldLE(): Heightfield {
        val field = Heightfield(width, depth, bounds, cellSize, cellHeight, borderSize)
        val stream = ByteArrayInputStream(spanData)
        for (fieldIndex in 0 until width * depth) {
            var prev: Span? = null
            val spanCount = stream.readLE16()
            repeat(spanCount) {
                val span = Span()
                span.min = stream.readLE32()
                span.max = stream.readLE32()
                span.area = stream.readLE32()
                if (prev == null) {
                    field.spans[fieldIndex] = span
                } else {
                    prev.next = span
                }
                prev = span
            }
        }
        return field
    }

    private fun serializeSpans(heightfield: Heightfield, order: ByteOrder): ByteArray {
        val counts = IntArray(heightfield.width * heightfield.height)
        val totalCount = serializeSpans0(heightfield, counts)
        return serializeSpans1(heightfield, order, counts, totalCount)
    }

    private fun serializeSpans0(heightfield: Heightfield, counts: IntArray): Int {
        var totalCount = 0
        var pz = 0
        repeat(heightfield.height) {
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
        repeat(heightfield.height) {
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
            repeat(l) {
                val count = buf.getShort().toInt()
                putShort(count, data, position, order)
                position += 2
                repeat(count) {
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