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
import org.recast4j.detour.tilecache.TileCacheBuilder
import org.recast4j.detour.tilecache.TileCacheParams
import java.io.OutputStream
import java.nio.ByteOrder

class TileCacheWriter : DetourWriter() {

    private val builder = TileCacheBuilder()

    fun write(stream: OutputStream, cache: TileCache, order: ByteOrder, cCompatibility: Boolean) {
        write(stream, TileCacheSetHeader.TILECACHESET_MAGIC, order)
        val version = if (cCompatibility) TileCacheSetHeader.TILECACHESET_VERSION
        else TileCacheSetHeader.TILECACHESET_VERSION_RECAST4J
        write(stream, version, order)
        var numTiles = 0
        for (i in 0 until cache.tileCount) {
            val tile = cache.getTile(i)
            if (tile?.data == null) continue
            numTiles++
        }
        write(stream, numTiles, order)
        NavMeshParamWriter.write(stream, cache.navMesh.params, order)
        writeCacheParams(stream, cache.params, order)
        for (i in 0 until cache.tileCount) {
            val tile = cache.getTile(i)
            if (tile?.data == null) continue
            write(stream, cache.getTileRef(tile).toInt(), order)
            val layer = cache.decompressTile(tile)
            val data = builder.compressTileCacheLayer(layer, order, cCompatibility)
            write(stream, data.size, order)
            stream.write(data)
        }
    }

    private fun writeCacheParams(stream: OutputStream, params: TileCacheParams, order: ByteOrder) {
        write(stream, params.orig, order)
        write(stream, params.cellSize, order)
        write(stream, params.cellHeight, order)
        write(stream, params.width, order)
        write(stream, params.height, order)
        write(stream, params.walkableHeight, order)
        write(stream, params.walkableRadius, order)
        write(stream, params.walkableClimb, order)
        write(stream, params.maxSimplificationError, order)
        write(stream, params.maxTiles, order)
        write(stream, params.maxObstacles, order)
    }
}