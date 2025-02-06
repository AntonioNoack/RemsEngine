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
        writeI32(stream, data.magic, order)
        writeI32(stream, MeshHeader.DT_NAVMESH_VERSION_RECAST4J_LAST, order)
        writeI32(stream, data.x, order)
        writeI32(stream, data.y, order)
        writeI32(stream, data.layer, order)
        writeI32(stream, data.userId, order)
        writeI32(stream, data.polyCount, order)
        writeI32(stream, data.vertCount, order)
        writeI32(stream, data.maxLinkCount, order)
        writeI32(stream, data.detailMeshCount, order)
        writeI32(stream, data.detailVertCount, order)
        writeI32(stream, data.detailTriCount, order)
        writeI32(stream, data.bvNodeCount, order)
        writeI32(stream, data.offMeshConCount, order)
        writeI32(stream, data.offMeshBase, order)
        writeF32(stream, data.walkableHeight, order)
        writeF32(stream, data.walkableRadius, order)
        writeF32(stream, data.walkableClimb, order)
        write(stream, data.bounds, order)
        writeF32(stream, data.bvQuantizationFactor, order)
        writeVertices(stream, data.vertices, data.vertCount, order)
        writePolys(stream, data, order)
        writePolyDetails(stream, data, order)
        writeVertices(stream, data.detailVertices, data.detailVertCount, order)
        writeDTris(stream, data)
        writeBVTree(stream, data, order)
        writeOffMeshCons(stream, data, order)
    }

    private fun writeVertices(stream: OutputStream, vertices: FloatArray, count: Int, order: ByteOrder) {
        for (i in 0 until count * 3) {
            writeF32(stream, vertices[i], order)
        }
    }

    private fun writePolys(stream: OutputStream, data: MeshData, order: ByteOrder) {
        for (i in 0 until data.polyCount) {
            val polygon = data.polygons[i]
            for (j in polygon.vertices.indices) {
                writeI16(stream, polygon.vertices[j].toShort(), order)
            }
            for (j in polygon.neighborData.indices) {
                writeI16(stream, polygon.neighborData[j].toShort(), order)
            }
            writeI16(stream, polygon.flags.toShort(), order)
            stream.write(polygon.vertCount)
            stream.write(polygon.areaAndType)
        }
    }

    private fun writePolyDetails(stream: OutputStream, data: MeshData, order: ByteOrder) {
        val detailMeshes = data.detailMeshes ?: return
        for (i in 0 until data.detailMeshCount) {
            val detailMesh = detailMeshes[i]
            writeI32(stream, detailMesh.vertBase, order)
            writeI32(stream, detailMesh.triBase, order)
            stream.write(detailMesh.vertCount)
            stream.write(detailMesh.triCount)
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
        writeI32(stream, node.minX, order)
        writeI32(stream, node.minY, order)
        writeI32(stream, node.minZ, order)
        writeI32(stream, node.maxX, order)
        writeI32(stream, node.maxY, order)
        writeI32(stream, node.maxZ, order)
        writeI32(stream, node.index, order)
    }

    private fun writeOffMeshCons(stream: OutputStream, data: MeshData, order: ByteOrder) {
        for (i in 0 until data.offMeshConCount) {
            writeOffMeshCon(stream, data.offMeshCons[i], order)
        }
    }

    private fun writeOffMeshCon(stream: OutputStream, con: OffMeshConnection, order: ByteOrder) {
        write(stream, con.posA, order)
        write(stream, con.posB, order)
        writeF32(stream, con.rad, order)
        writeI16(stream, con.poly.toShort(), order)
        stream.write(con.flags)
        stream.write(con.side.toInt())
        writeI32(stream, con.userId, order)
    }
}