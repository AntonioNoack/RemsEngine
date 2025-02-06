/*
Recast4J Copyright (c) 2015 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour.io

import org.recast4j.detour.NavMesh
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MeshSetReader {

    fun read(input: InputStream, maxVertPerPoly: Int): NavMesh {
        return read(IOUtils.toByteBuffer(input), maxVertPerPoly)
    }

    fun read(input: InputStream): NavMesh {
        return read(IOUtils.toByteBuffer(input))
    }

    fun read(bb: ByteBuffer): NavMesh {
        return read(bb, -1)
    }

    fun read(bb: ByteBuffer, maxVertPerPoly: Int): NavMesh {
        val header = readHeader(bb, maxVertPerPoly)
        if (header.maxVerticesPerPoly <= 0) {
            throw IOException("Invalid number of vertices per poly " + header.maxVerticesPerPoly)
        }
        val cCompatibility = header.version == NavMeshSetHeader.NAVMESHSET_VERSION
        val mesh = NavMesh(header.params, header.maxVerticesPerPoly)
        readTiles(bb, header, cCompatibility, mesh)
        return mesh
    }

    private fun readHeader(bb: ByteBuffer, maxVerticesPerPoly: Int): NavMeshSetHeader {
        val header = NavMeshSetHeader()
        header.magic = bb.int
        if (header.magic != NavMeshSetHeader.NAVMESHSET_MAGIC) {
            header.magic = IOUtils.swapEndianness(header.magic)
            if (header.magic != NavMeshSetHeader.NAVMESHSET_MAGIC) {
                throw IOException("Invalid magic " + header.magic)
            }
            bb.order(if (bb.order() == ByteOrder.BIG_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        }
        header.version = bb.int
        if (header.version != NavMeshSetHeader.NAVMESHSET_VERSION && header.version != NavMeshSetHeader.NAVMESHSET_VERSION_RECAST4J_1 && header.version != NavMeshSetHeader.NAVMESHSET_VERSION_RECAST4J) {
            throw IOException("Invalid version " + header.version)
        }
        header.numTiles = bb.int
        header.params = NavMeshParamReader.read(bb)
        header.maxVerticesPerPoly = maxVerticesPerPoly
        if (header.version == NavMeshSetHeader.NAVMESHSET_VERSION_RECAST4J) {
            header.maxVerticesPerPoly = bb.int
        }
        return header
    }

    private fun readTiles(
        bb: ByteBuffer,
        header: NavMeshSetHeader,
        cCompatibility: Boolean,
        mesh: NavMesh
    ) {
        // Read tiles.
        for (i in 0 until header.numTiles) {
            val tileHeader = NavMeshTileHeader()
            tileHeader.tileRef = bb.long
            tileHeader.dataSize = bb.int
            if (tileHeader.tileRef == 0L || tileHeader.dataSize == 0) {
                break
            }
            if (cCompatibility) {
                bb.getInt() // C struct padding
            }
            val data = MeshDataReader.read(bb, mesh.maxVerticesPerPoly)
            mesh.addTile(data, i, tileHeader.tileRef)
        }
    }
}