package me.anno.tests.mesh.minecraft

import me.anno.utils.types.BufferInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream

// .MCR
object RegionFormatReader {

    class RegionSection {
        val chunks = arrayOfNulls<RegionChunk>(1024)
        override fun toString(): String {
            return chunks.withIndex().filter { it.value != null }
                .joinToString { (idx, ch) -> "${(idx shr 5)},${idx and 31}: $ch" }
        }
    }

    class RegionChunk(private val offset: Int, private val bytes: ByteBuffer) {
        private val exactLength get() = bytes.getInt(offset)
        private val compression get() = bytes.get(offset + 4).toInt() // only format 1 is used according to docs
        val properties by lazy {
            if (compression != 2) throw NotImplementedError("Unknown compression")
            // ZLib... what is that???
            val nbtRaw = InflaterInputStream(BufferInputStream(bytes, offset + 5, exactLength - 1))
            val map0 = NBTReader.read(DataInputStream(nbtRaw), 10) as Map<String, Any>
            if (map0.size != 1) throw IllegalStateException("Expected one element")
            map0[""] as Map<String, Any>
            // -> this then would be NBT data
        }
    }

    fun read(data: ByteBuffer): RegionSection {
        data.order(ByteOrder.BIG_ENDIAN)
        val section = RegionSection()
        for (i in 0 until 1024) {
            val value = data.getInt()
            if (value != 0) {
                val offset = value.ushr(8).shl(12)
                // val size = value.and(0xff).shl(12)
                section.chunks[i] = RegionChunk(offset, data)
            }// else not generated
        }
        return section
    }
}