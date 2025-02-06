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

import org.recast4j.detour.tilecache.TileCacheLayerHeader
import java.io.IOException
import java.nio.ByteBuffer

object TileCacheLayerHeaderReader {
    fun <V : TileCacheLayerHeader> read(data: ByteBuffer, cCompatibility: Boolean, header: V): V {
        header.magic = data.getInt()
        header.version = data.getInt()
        if (header.magic != TileCacheLayerHeader.DT_TILECACHE_MAGIC) throw IOException("Invalid magic")
        if (header.version != TileCacheLayerHeader.DT_TILECACHE_VERSION) throw IOException("Invalid version")
        header.tx = data.getInt()
        header.ty = data.getInt()
        header.tlayer = data.getInt()
        header.bmin.set(data.getFloat(), data.getFloat(), data.getFloat())
        header.bmax.set(data.getFloat(), data.getFloat(), data.getFloat())
        header.hmin = data.uint16()
        header.hmax = data.uint16()
        header.width = data.uint8()
        header.height = data.uint8()
        header.minx = data.uint8()
        header.maxx = data.uint8()
        header.miny = data.uint8()
        header.maxy = data.uint8()
        if (cCompatibility) {
            data.getShort() // C struct padding
        }
        return header
    }

    fun ByteBuffer.uint8() = get().toInt() and 0xFF
    fun ByteBuffer.uint16() = getShort().toInt() and 0xFF
}