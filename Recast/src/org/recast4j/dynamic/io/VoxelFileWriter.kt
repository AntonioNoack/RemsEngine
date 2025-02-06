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

import org.recast4j.detour.io.DetourWriter
import java.io.OutputStream
import java.nio.ByteOrder

object VoxelFileWriter : DetourWriter() {

    fun write(stream: OutputStream, f: VoxelFile, byteOrder: ByteOrder = VoxelFile.PREFERRED_BYTE_ORDER) {
        writeI32(stream, VoxelFile.MAGIC, byteOrder)
        writeI32(stream, VoxelFile.VERSION_EXPORTER_RECAST4J, byteOrder)
        writeF32(stream, f.walkableRadius, byteOrder)
        writeF32(stream, f.walkableHeight, byteOrder)
        writeF32(stream, f.walkableClimb, byteOrder)
        writeF32(stream, f.walkableSlopeAngle, byteOrder)
        writeF32(stream, f.cellSize, byteOrder)
        writeF32(stream, f.maxSimplificationError, byteOrder)
        writeF32(stream, f.maxEdgeLen, byteOrder)
        writeF32(stream, f.minRegionArea, byteOrder)
        writeF32(stream, f.regionMergeArea, byteOrder)
        writeI32(stream, f.verticesPerPoly, byteOrder)
        write(stream, f.buildMeshDetail)
        writeF32(stream, f.detailSampleDistance, byteOrder)
        writeF32(stream, f.detailSampleMaxError, byteOrder)
        write(stream, f.useTiles)
        writeI32(stream, f.tileSizeX, byteOrder)
        writeI32(stream, f.tileSizeZ, byteOrder)
        write(stream, f.rotation, byteOrder)
        writeF32(stream, f.bounds.minX, byteOrder)
        writeF32(stream, f.bounds.minY, byteOrder)
        writeF32(stream, f.bounds.minZ, byteOrder)
        writeF32(stream, f.bounds.maxX, byteOrder)
        writeF32(stream, f.bounds.maxY, byteOrder)
        writeF32(stream, f.bounds.maxZ, byteOrder)
        writeI32(stream, f.tiles.size, byteOrder)
        for (t in f.tiles) {
            writeTile(stream, t, byteOrder)
        }
    }

    fun writeTile(stream: OutputStream, tile: VoxelTile, byteOrder: ByteOrder) {
        writeI32(stream, tile.tileX, byteOrder)
        writeI32(stream, tile.tileZ, byteOrder)
        writeI32(stream, tile.width, byteOrder)
        writeI32(stream, tile.depth, byteOrder)
        writeI32(stream, tile.borderSize, byteOrder)
        write(stream, tile.bounds, byteOrder)
        writeF32(stream, tile.cellSize, byteOrder)
        writeF32(stream, tile.cellHeight, byteOrder)
        val bytes = tile.spanData
        writeI32(stream, bytes.size, byteOrder)
        stream.write(bytes)
    }
}