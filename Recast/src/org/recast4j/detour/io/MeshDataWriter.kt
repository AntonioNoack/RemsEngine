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

import org.recast4j.detour.BVNode
import org.recast4j.detour.MeshData
import org.recast4j.detour.MeshHeader
import org.recast4j.detour.OffMeshConnection
import java.io.OutputStream
import java.nio.ByteOrder

object MeshDataWriter : DetourWriter() {

    fun write(stream: OutputStream, data: MeshData, order: ByteOrder) {
        val header = data
        write(stream, header.magic, order)
        write(stream, MeshHeader.DT_NAVMESH_VERSION_RECAST4J_LAST, order)
        write(stream, header.x, order)
        write(stream, header.y, order)
        write(stream, header.layer, order)
        write(stream, header.userId, order)
        write(stream, header.polyCount, order)
        write(stream, header.vertCount, order)
        write(stream, header.maxLinkCount, order)
        write(stream, header.detailMeshCount, order)
        write(stream, header.detailVertCount, order)
        write(stream, header.detailTriCount, order)
        write(stream, header.bvNodeCount, order)
        write(stream, header.offMeshConCount, order)
        write(stream, header.offMeshBase, order)
        write(stream, header.walkableHeight, order)
        write(stream, header.walkableRadius, order)
        write(stream, header.walkableClimb, order)
        write(stream, header.bmin, order)
        write(stream, header.bmax, order)
        write(stream, header.bvQuantizationFactor, order)
        writeVertices(stream, data.vertices, header.vertCount, order)
        writePolys(stream, data, order)
        writePolyDetails(stream, data, order)
        writeVertices(stream, data.detailVertices, header.detailVertCount, order)
        writeDTris(stream, data)
        writeBVTree(stream, data, order)
        writeOffMeshCons(stream, data, order)
    }

    private fun writeVertices(stream: OutputStream, vertices: FloatArray, count: Int, order: ByteOrder) {
        for (i in 0 until count * 3) {
            write(stream, vertices[i], order)
        }
    }

    private fun writePolys(stream: OutputStream, data: MeshData, order: ByteOrder) {
        for (i in 0 until data.polyCount) {
            for (j in data.polygons[i].vertices.indices) {
                write(stream, data.polygons[i].vertices[j].toShort(), order)
            }
            for (j in data.polygons[i].neighborData.indices) {
                write(stream, data.polygons[i].neighborData[j].toShort(), order)
            }
            write(stream, data.polygons[i].flags.toShort(), order)
            stream.write(data.polygons[i].vertCount)
            stream.write(data.polygons[i].areaAndType)
        }
    }

    private fun writePolyDetails(stream: OutputStream, data: MeshData, order: ByteOrder) {
        for (i in 0 until data.detailMeshCount) {
            write(stream, data.detailMeshes!![i].vertBase, order)
            write(stream, data.detailMeshes!![i].triBase, order)
            stream.write(data.detailMeshes!![i].vertCount)
            stream.write(data.detailMeshes!![i].triCount)
        }
    }

    private fun writeDTris(stream: OutputStream, data: MeshData) {
        stream.write(data.detailTriangles, 0, data.detailTriCount * 4)
    }

    private fun writeBVTree(stream: OutputStream, data: MeshData, order: ByteOrder) {
        val tree = data.bvTree ?: return
        for (i in 0 until data.bvNodeCount) {
            val node = tree[i]
            writeBVNode(stream, node, order)
        }
    }

    private fun writeBVNode(stream: OutputStream, node: BVNode, order: ByteOrder) {
        write(stream, node.minX, order)
        write(stream, node.minY, order)
        write(stream, node.minZ, order)
        write(stream, node.maxX, order)
        write(stream, node.maxY, order)
        write(stream, node.maxZ, order)
        write(stream, node.index, order)
    }

    private fun writeOffMeshCons(stream: OutputStream, data: MeshData, order: ByteOrder) {
        for (i in 0 until data.offMeshConCount) {
            writeOffMeshCon(stream, data.offMeshCons[i], order)
        }
    }

    private fun writeOffMeshCon(stream: OutputStream, con: OffMeshConnection, order: ByteOrder) {
        write(stream, con.posA, order)
        write(stream, con.posB, order)
        write(stream, con.rad, order)
        write(stream, con.poly.toShort(), order)
        stream.write(con.flags)
        stream.write(con.side)
        write(stream, con.userId, order)
    }
}