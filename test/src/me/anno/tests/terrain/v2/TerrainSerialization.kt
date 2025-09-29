package me.anno.tests.terrain.v2

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainChunk
import me.anno.io.binary.ByteArrayIO.beMagic
import me.anno.io.binary.ByteArrayIO.readBE32
import me.anno.io.binary.ByteArrayIO.readBE32F
import me.anno.io.binary.ByteArrayIO.writeBE32
import me.anno.io.files.FileReference
import me.anno.tests.LOGGER
import me.anno.tests.terrain.v2.TerrainLoaderV2.Companion.createChunk
import me.anno.tests.terrain.v2.TerrainLoaderV2.VirtualChunk
import me.anno.tests.terrain.v2.TerrainLoaderV2.VirtualChunkId
import me.anno.utils.assertions.assertEquals
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.arrays.ByteArrayList

object TerrainSerialization {

    private val MAGIC = beMagic("TRIT")
    private const val VERSION_1_0 = 0x1000
    private const val HEADER_SIZE = 12
    private const val ENTRY_SIZE = 20

    fun saveTerrain(chunks: Map<VirtualChunkId, VirtualChunk>, file: FileReference) {
        val entries =
            chunks.entries.toList()
        val headerSize = HEADER_SIZE + entries.size * ENTRY_SIZE
        val capacity = headerSize + entries.sumOf {
            val mesh = it.value.mesh
            mesh.positions!!.size / 3 * 24 + mesh.indices!!.size / 3 * 12
        }
        val bos = ByteArrayList(capacity)
        bos.writeBE32(MAGIC)
        bos.writeBE32(VERSION_1_0)
        bos.writeBE32(entries.size)
        for (i in entries.indices) {
            val (key, value) = entries[i]
            val numPos = value.mesh.positions!!.size / 3
            val numTris = value.mesh.indices!!.size / 3
            bos.writeBE32(key.xi)
            bos.writeBE32(key.zi)
            bos.writeBE32(key.lod)
            bos.writeBE32(numPos)
            bos.writeBE32(numTris)
        }
        for (i in entries.indices) {
            val value = entries[i].value
            val positions = value.mesh.positions!!
            val normals = value.mesh.normals!!
            val indices = value.mesh.indices!!
            assertEquals(positions.size, normals.size)
            for (i in positions.indices) {
                bos.writeBE32(positions[i])
            }
            for (i in normals.indices) {
                bos.writeBE32(normals[i])
            }
            for (i in indices.indices) {
                bos.writeBE32(indices[i])
            }
        }
        assertEquals(bos.size, capacity)
        file.writeBytes(bos)
        LOGGER.info("Saved $file, ${bos.size.formatFileSize()}")
    }

    fun loadTerrain(
        chunks: HashMap<VirtualChunkId, VirtualChunk>,
        bytes: ByteArray, terrain: TriTerrainChunk
    ): Boolean {

        if (bytes.size < HEADER_SIZE) return false
        val magic = bytes.readBE32(0)
        val version = bytes.readBE32(4)
        if (version != VERSION_1_0 || magic != MAGIC) return false
        val numEntries = bytes.readBE32(8)
        val headerSize = HEADER_SIZE + numEntries * ENTRY_SIZE
        if (bytes.size < headerSize) return false

        var dataPtr = headerSize
        for (i in 0 until numEntries) {
            val headerOffset = HEADER_SIZE + i * ENTRY_SIZE
            val xi = bytes.readBE32(headerOffset)
            val zi = bytes.readBE32(headerOffset + 4)
            val lod = bytes.readBE32(headerOffset + 8)
            val numPos = bytes.readBE32(headerOffset + 12)
            val numTris = bytes.readBE32(headerOffset + 16)
            val dataSize = numPos * 24 + numTris * 12
            if (bytes.size < dataPtr + dataSize) return false
            val expectedEnd = dataPtr + dataSize

            val positions = FloatArray(numPos * 3)
            val normals = FloatArray(numPos * 3)
            val indices = IntArray(numTris * 3)

            for (i in positions.indices) {
                positions[i] = bytes.readBE32F(dataPtr)
                dataPtr += 4
            }

            for (i in normals.indices) {
                normals[i] = bytes.readBE32F(dataPtr)
                dataPtr += 4
            }

            for (i in indices.indices) {
                indices[i] = bytes.readBE32(dataPtr)
                dataPtr += 4
            }

            val id = VirtualChunkId(xi, zi, lod)
            val mesh = Mesh()
            mesh.positions = positions
            mesh.normals = normals
            mesh.indices = indices

            val previous = chunks.put(id, createChunk(id, mesh))
            if (previous != null) terrain.remove(previous.mesh)
            terrain.addMesh(mesh, true)

            assertEquals(expectedEnd, dataPtr)
        }

        return true
    }
}