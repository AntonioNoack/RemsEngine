/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour.tilecache.io

import org.recast4j.detour.io.DetourWriter
import org.recast4j.detour.io.NavMeshParamWriter
import org.recast4j.detour.tilecache.TileCache
import org.recast4j.detour.tilecache.builder.TileCacheBuilder
import org.recast4j.detour.tilecache.TileCacheParams
import java.io.OutputStream
import java.nio.ByteOrder

object TileCacheWriter : DetourWriter() {

    fun write(stream: OutputStream, cache: TileCache, order: ByteOrder, cCompatibility: Boolean) {
        writeI32(stream, TileCacheSetHeader.TILECACHESET_MAGIC, order)
        val version = if (cCompatibility) TileCacheSetHeader.TILECACHESET_VERSION
        else TileCacheSetHeader.TILECACHESET_VERSION_RECAST4J
        writeI32(stream, version, order)
        var numTiles = 0
        for (i in 0 until cache.tileCount) {
            val tile = cache.getTile(i)
            if (tile?.data == null) continue
            numTiles++
        }
        writeI32(stream, numTiles, order)
        NavMeshParamWriter.write(stream, cache.navMesh.params, order)
        writeCacheParams(stream, cache.params, order)
        for (i in 0 until cache.tileCount) {
            val tile = cache.getTile(i)
            if (tile?.data == null) continue
            writeI32(stream, cache.getTileRef(tile).toInt(), order)
            val layer = cache.decompressTile(tile)
            val data = TileCacheBuilder.compressTileCacheLayer(layer, order, cCompatibility)
            writeI32(stream, data.size, order)
            stream.write(data)
        }
    }

    private fun writeCacheParams(stream: OutputStream, params: TileCacheParams, order: ByteOrder) {
        write(stream, params.orig, order)
        writeF32(stream, params.cellSize, order)
        writeF32(stream, params.cellHeight, order)
        writeI32(stream, params.width, order)
        writeI32(stream, params.height, order)
        writeF32(stream, params.walkableHeight, order)
        writeF32(stream, params.walkableRadius, order)
        writeF32(stream, params.walkableClimb, order)
        writeF32(stream, params.maxSimplificationError, order)
        writeI32(stream, params.maxTiles, order)
        writeI32(stream, params.maxObstacles, order)
    }
}