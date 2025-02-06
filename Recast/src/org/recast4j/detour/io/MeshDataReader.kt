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

import org.recast4j.detour.*
import org.recast4j.detour.tilecache.io.TileCacheLayerHeaderReader.uint16
import org.recast4j.detour.tilecache.io.TileCacheLayerHeaderReader.uint8
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MeshDataReader {

    fun read(stream: InputStream, maxVertPerPoly: Int): MeshData {
        val buf = IOUtils.toByteBuffer(stream)
        return read(buf, maxVertPerPoly)
    }

    fun read(buf: ByteBuffer, maxVertPerPoly: Int): MeshData {
        val data = MeshData()
        data.magic = buf.getInt()
        if (data.magic != MeshHeader.DT_NAVMESH_MAGIC) {
            data.magic = IOUtils.swapEndianness(data.magic)
            if (data.magic != MeshHeader.DT_NAVMESH_MAGIC) {
                throw IOException("Invalid magic")
            }
            buf.order(if (buf.order() == ByteOrder.BIG_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        }
        data.version = buf.getInt()
        if (data.version != MeshHeader.DT_NAVMESH_VERSION) {
            if (data.version < MeshHeader.DT_NAVMESH_VERSION_RECAST4J_FIRST
                || data.version > MeshHeader.DT_NAVMESH_VERSION_RECAST4J_LAST
            ) {
                throw IOException("Invalid version " + data.version)
            }
        }
        val cCompatibility = data.version == MeshHeader.DT_NAVMESH_VERSION
        data.x = buf.getInt()
        data.y = buf.getInt()
        data.layer = buf.getInt()
        data.userId = buf.getInt()
        data.polyCount = buf.getInt()
        data.vertCount = buf.getInt()
        data.maxLinkCount = buf.getInt()
        data.detailMeshCount = buf.getInt()
        data.detailVertCount = buf.getInt()
        data.detailTriCount = buf.getInt()
        data.bvNodeCount = buf.getInt()
        data.offMeshConCount = buf.getInt()
        data.offMeshBase = buf.getInt()
        data.walkableHeight = buf.getFloat()
        data.walkableRadius = buf.getFloat()
        data.walkableClimb = buf.getFloat()
        data.bmin.set(buf.getFloat(), buf.getFloat(), buf.getFloat())
        data.bmax.set(buf.getFloat(), buf.getFloat(), buf.getFloat())
        data.bvQuantizationFactor = buf.getFloat()
        data.vertices = readVertices(buf, data.vertCount)
        data.polygons = readPolys(buf, data, maxVertPerPoly)
        if (cCompatibility) {
            buf.position(buf.position() + data.maxLinkCount * LINK_SIZEOF)
        }
        data.detailMeshes = readPolyDetails(buf, data, cCompatibility)
        data.detailVertices = readVertices(buf, data.detailVertCount)
        data.detailTriangles = readDTris(buf, data)
        data.bvTree = readBVTree(buf, data)
        data.offMeshCons = readOffMeshCons(buf, data)
        return data
    }

    private fun readVertices(buf: ByteBuffer, count: Int): FloatArray {
        val vertices = FloatArray(count * 3)
        for (i in vertices.indices) {
            vertices[i] = buf.getFloat()
        }
        return vertices
    }

    private fun readPolys(buf: ByteBuffer, header: MeshHeader, maxVertPerPoly: Int): Array<Poly> {
        val polys = Array(header.polyCount) { Poly(it, maxVertPerPoly) }
        for (i in polys.indices) {
            if (header.version < MeshHeader.DT_NAVMESH_VERSION_RECAST4J_NO_POLY_FIRSTLINK) {
                buf.getInt() // polys[i].getFirst()Link
            }
            for (j in polys[i].vertices.indices) {
                polys[i].vertices[j] = buf.uint16()
            }
            for (j in polys[i].neighborData.indices) {
                polys[i].neighborData[j] = buf.uint16()
            }
            polys[i].flags = buf.uint16()
            polys[i].vertCount = buf.uint8()
            polys[i].areaAndType = buf.uint8()
        }
        return polys
    }

    private fun readPolyDetails(buf: ByteBuffer, header: MeshHeader, cCompatibility: Boolean): Array<PolyDetail> {
        val polys = Array(header.detailMeshCount) { PolyDetail() }
        for (i in polys.indices) {
            polys[i].vertBase = buf.getInt()
            polys[i].triBase = buf.getInt()
            polys[i].vertCount = buf.uint8()
            polys[i].triCount = buf.uint8()
            if (cCompatibility) {
                buf.getShort() // C struct padding
            }
        }
        return polys
    }

    private fun readDTris(buf: ByteBuffer, header: MeshHeader): ByteArray {
        val bytes = ByteArray(4 * header.detailTriCount)
        buf.get(bytes)
        return bytes
    }

    private fun readBVTree(buf: ByteBuffer, header: MeshHeader): Array<BVNode> {
        val nodes = Array(header.bvNodeCount) { BVNode() }
        for (i in nodes.indices) {
            readBVNode(buf, nodes[i])
        }
        return nodes
    }

    private fun readBVNode(buf: ByteBuffer, n: BVNode) {
        n.minX = buf.getInt()
        n.minY = buf.getInt()
        n.minZ = buf.getInt()
        n.maxX = buf.getInt()
        n.maxY = buf.getInt()
        n.maxZ = buf.getInt()
        n.index = buf.getInt()
    }

    private fun readOffMeshCons(buf: ByteBuffer, header: MeshHeader): Array<OffMeshConnection> {
        val cons = Array(header.offMeshConCount) { OffMeshConnection() }
        for (i in cons.indices) {
            readOffMeshCon(buf, cons[i])
        }
        return cons
    }

    private fun readOffMeshCon(buf: ByteBuffer, con: OffMeshConnection) {
        con.posA.set(buf.getFloat(), buf.getFloat(), buf.getFloat())
        con.posB.set(buf.getFloat(), buf.getFloat(), buf.getFloat())
        con.rad = buf.getFloat()
        con.poly = buf.getShort().toInt() and 0xffff
        con.flags = buf.uint8()
        con.side = buf.uint8()
        con.userId = buf.getInt()
    }

    const val LINK_SIZEOF = 16
}