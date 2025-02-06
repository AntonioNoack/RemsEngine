/*
Recast4J Copyright (c) 2015-2018 Piotr Piastucki piotr@jtilia.org

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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteOrder

object MeshSetWriter : DetourWriter() {

    fun write(stream: OutputStream, mesh: NavMesh, order: ByteOrder) {
        writeHeader(stream, mesh, order)
        writeTiles(stream, mesh, order)
    }

    private fun writeHeader(stream: OutputStream, mesh: NavMesh, order: ByteOrder) {
        write(stream, NavMeshSetHeader.NAVMESHSET_MAGIC, order)
        write(stream, NavMeshSetHeader.NAVMESHSET_VERSION_RECAST4J, order)
        write(stream, mesh.numTiles, order)
        NavMeshParamWriter.write(stream, mesh.params, order)
        write(stream, mesh.maxVerticesPerPoly, order)
    }

    private fun writeTiles(stream: OutputStream, mesh: NavMesh, order: ByteOrder) {
        for (tile in mesh.allTiles) {
            val tileHeader = NavMeshTileHeader()
            tileHeader.tileRef = mesh.getTileRef(tile)
            val baos = ByteArrayOutputStream()
            MeshDataWriter.write(baos, tile.data, order)
            val ba = baos.toByteArray()
            tileHeader.dataSize = ba.size
            write(stream, tileHeader.tileRef, order)
            write(stream, tileHeader.dataSize, order)
            stream.write(ba)
        }
    }
}