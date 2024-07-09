package me.anno.tests.mesh.hexagons

import me.anno.io.Streams.read0String
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.readBE64
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.write0String
import me.anno.io.Streams.writeBE32
import me.anno.io.Streams.writeBE64
import me.anno.io.files.FileReference
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.structures.lists.Lists.createArrayList
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

typealias ChunkS = HashMap<Long, ByteArray>
typealias TriangleS = HashMap<Long, ChunkS>

class HexMCWorldSave {

    val magic = "HexSphere!!" // 12 bytes with \0
    val version = 0
    val triangles = createArrayList(20) { TriangleS() }
    var sy = 64

    operator fun get(sphere: HexagonSphere, key: Hexagon): ByteArray? {
        val sc = sphere.findChunk(key)
        val tri = triangles[sc.tri]
        val sci = sc.si + sc.sj.toLong() * sphere.chunkCount
        return tri[sci]?.get(key.index)
    }

    operator fun set(sphere: HexagonSphere, key: Hexagon, value: ByteArray?) {
        val sc = sphere.findChunk(key)
        val tri = triangles[sc.tri]
        val sci = sc.si + sc.sj.toLong() * sphere.chunkCount
        if (value != null) {
            tri.getOrPut(sci) { ChunkS() }[key.index] = value
        } else {
            tri[sci]?.remove(key.index)
        }
    }

    fun read(file: FileReference) {
        file.inputStream { it, exc ->
            if (exc != null) throw exc
            it!!
            if (it.read0String() != magic)
                throw IOException("Incorrect magic")
            if (it.readBE32() != version)
                throw IOException("Incompatible version")
            sy = it.readBE32()
            val it2 = InflaterInputStream(it).buffered()
            for (tri in triangles) {
                read(it2, tri)
            }
            it2.close()
        }
    }

    fun read(it: InputStream, tri: TriangleS) {
        tri.clear()
        val len = it.readBE32()
        for (i in 0 until len) {
            tri[it.readBE64()] = read(it)
        }
    }

    fun read(it: InputStream): ChunkS {
        val len = it.readBE32()
        val sub = ChunkS(len)
        for (i in 0 until len) {
            sub[it.readBE64()] = it.readNBytes2(sy, true)
        }
        return sub
    }

    fun write(world: HexagonSphereMCWorld, file: FileReference) {
        val it = file.outputStream()
        sy = world.sy
        it.write0String(magic)
        it.writeBE32(version)
        it.writeBE32(sy)
        val it2 = DeflaterOutputStream(it)
        for (tri in triangles) {
            write(it2, tri)
        }
        it2.close()
    }

    fun write(it: OutputStream, tri: TriangleS) {
        val len = tri.size
        it.writeBE32(len)
        for ((key, value) in tri) {
            it.writeBE64(key)
            write1(it, value)
        }
    }

    fun write1(it: OutputStream, tri: ChunkS) {
        val len = tri.size
        it.writeBE32(len)
        for ((key, value) in tri) {
            it.writeBE64(key)
            it.write(value, 0, sy)
        }
    }
}