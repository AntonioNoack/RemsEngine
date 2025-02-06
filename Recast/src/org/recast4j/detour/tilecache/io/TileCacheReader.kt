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

import org.recast4j.detour.NavMesh
import org.recast4j.detour.io.IOUtils
import org.recast4j.detour.io.NavMeshParamReader
import org.recast4j.detour.tilecache.TileCache
import org.recast4j.detour.tilecache.TileCacheMeshProcess
import org.recast4j.detour.tilecache.TileCacheParams
import org.recast4j.detour.tilecache.TileCacheStorageParams
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TileCacheReader {
    fun read(s: InputStream, maxVertPerPoly: Int, meshProcessor: TileCacheMeshProcess?): TileCache {
        val bb = IOUtils.toByteBuffer(s)
        return read(bb, maxVertPerPoly, meshProcessor)
    }

    fun read(bb: ByteBuffer, maxVertPerPoly: Int, meshProcessor: TileCacheMeshProcess?): TileCache {
        val header = TileCacheSetHeader()
        header.magic = bb.getInt()
        if (header.magic != TileCacheSetHeader.TILECACHESET_MAGIC) {
            header.magic = IOUtils.swapEndianness(header.magic)
            if (header.magic != TileCacheSetHeader.TILECACHESET_MAGIC) {
                throw IOException("Invalid magic")
            }
            bb.order(if (bb.order() == ByteOrder.BIG_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        }
        header.version = bb.getInt()
        if (header.version != TileCacheSetHeader.TILECACHESET_VERSION) {
            if (header.version != TileCacheSetHeader.TILECACHESET_VERSION_RECAST4J) {
                throw IOException("Invalid version")
            }
        }
        val cCompatibility = header.version == TileCacheSetHeader.TILECACHESET_VERSION
        header.numTiles = bb.getInt()
        header.meshParams =  NavMeshParamReader.read(bb)
        header.cacheParams = readCacheParams(bb)
        val mesh = NavMesh(header.meshParams, maxVertPerPoly)
        val tc = TileCache(header.cacheParams, TileCacheStorageParams(bb.order(), cCompatibility), mesh, meshProcessor)
        // Read tiles.
        for (i in 0 until header.numTiles) {
            val tileRef = bb.getInt().toLong()
            val dataSize = bb.getInt()
            if (tileRef == 0L || dataSize == 0) {
                break
            }
            val data = ByteArray(dataSize)
            bb[data]
            val tile = tc.addTile(data, 0)
            if (tile != 0L) {
                tc.buildNavMeshTile(tile)
            }
        }
        return tc
    }

    private fun readCacheParams(bb: ByteBuffer): TileCacheParams {
        val params = TileCacheParams()
        params.orig.set(bb.float, bb.float, bb.float)
        params.cellSize = bb.float
        params.cellHeight = bb.float
        params.width = bb.getInt()
        params.height = bb.getInt()
        params.walkableHeight = bb.float
        params.walkableRadius = bb.float
        params.walkableClimb = bb.float
        params.maxSimplificationError = bb.float
        params.maxTiles = bb.getInt()
        params.maxObstacles = bb.getInt()
        return params
    }
}