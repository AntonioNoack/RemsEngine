/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.recast.geom

import org.joml.AABBf
import org.recast4j.detour.NavMeshDataCreateParams.Companion.i0
import java.util.Arrays
import kotlin.math.min

class ChunkyTriMesh(vertices: FloatArray, indices: IntArray, numTris: Int, trisPerChunk: Int) {

    class Node {
        var minX = 0f
        var minY = 0f
        var maxX = 0f
        var maxY = 0f
        var i = 0

        // empty for branches
        var triangles: IntArray = i0
    }

    private object CompareItemX : Comparator<Node> {
        override fun compare(a: Node, b: Node): Int {
            return a.minX.compareTo(b.minX)
        }
    }

    private object CompareItemY : Comparator<Node> {
        override fun compare(a: Node, b: Node): Int {
            return a.minY.compareTo(b.minY)
        }
    }

    private fun calcExtents(items: Array<Node>, startIndex: Int, endIndex: Int, dst: Node) {
        var n = items[startIndex]
        dst.minX = n.minX
        dst.minY = n.minY
        dst.maxX = n.maxX
        dst.maxY = n.maxY
        for (i in startIndex + 1 until endIndex) {
            n = items[i]
            dst.minX = min(dst.minX, n.minX)
            dst.minY = min(dst.minY, n.minY)
            dst.maxX = min(dst.maxX, n.maxX)
            dst.maxY = min(dst.maxY, n.maxY)
        }
    }

    private fun longestAxis(x: Float, y: Float): Int {
        return if (y > x) 1 else 0
    }

    private fun subdivide(
        items: Array<Node>,
        startIndex: Int,
        endIndex: Int,
        trisPerChunk: Int,
        nodes: MutableList<Node>,
        inTris: IntArray
    ) {
        val length = endIndex - startIndex
        val node = Node()
        nodes.add(node)
        if (length <= trisPerChunk) {

            // Leaf
            calcExtents(items, startIndex, endIndex, node)

            // Copy triangles.
            node.i = nodes.size
            node.triangles = IntArray(length * 3)
            var dst = 0
            for (i in startIndex until endIndex) {
                val src = items[i].i * 3
                node.triangles[dst++] = inTris[src]
                node.triangles[dst++] = inTris[src + 1]
                node.triangles[dst++] = inTris[src + 2]
            }
        } else {

            // Split
            calcExtents(items, startIndex, endIndex, node)
            val axis = longestAxis(node.maxX - node.minX, node.maxY - node.minY)
            if (axis == 0) {
                Arrays.sort(items, startIndex, endIndex, CompareItemX)
                // Sort along x-axis
            } else if (axis == 1) {
                Arrays.sort(items, startIndex, endIndex, CompareItemY)
                // Sort along y-axis
            }
            val splitIndex = startIndex + length / 2

            // Left
            subdivide(items, startIndex, splitIndex, trisPerChunk, nodes, inTris)
            // Right
            subdivide(items, splitIndex, endIndex, trisPerChunk, nodes, inTris)

            // Negative index means escape.
            node.i = -nodes.size
        }
    }

    val numChunks = (numTris + trisPerChunk - 1) / trisPerChunk
    val nodes = ArrayList<Node>(numChunks)
    var numTriangles = numTris
    var maxTrisPerChunk: Int

    init {
        val items = buildTree(numTris, vertices, indices)
        subdivide(items, 0, numTris, trisPerChunk, nodes, indices)
        maxTrisPerChunk = calculateMaxTrisPerChunk()
    }

    private fun buildTree(numTris: Int, vertices: FloatArray, tris: IntArray): Array<Node> {
        return Array(numTris) { i -> buildTreeNode(i, vertices, tris) }
    }

    private fun buildTreeNode(i: Int, vertices: FloatArray, tris: IntArray): Node {
        val t = i * 3
        val it = Node()
        it.i = i
        // Calc triangle XZ bounds.
        it.maxX = vertices[tris[t] * 3]
        it.minX = it.maxX
        it.maxY = vertices[tris[t] * 3 + 2]
        it.minY = it.maxY
        for (j in 1..2) {
            val v = tris[t + j] * 3
            if (vertices[v] < it.minX) {
                it.minX = vertices[v]
            }
            if (vertices[v + 2] < it.minY) {
                it.minY = vertices[v + 2]
            }
            if (vertices[v] > it.maxX) {
                it.maxX = vertices[v]
            }
            if (vertices[v + 2] > it.maxY) {
                it.maxY = vertices[v + 2]
            }
        }
        return it
    }

    private fun calculateMaxTrisPerChunk(): Int {
        var maxTrisPerChunk = 0
        for (node in nodes) {
            val isLeaf = node.i >= 0
            if (!isLeaf) {
                continue
            }
            if (node.triangles.size / 3 > maxTrisPerChunk) {
                maxTrisPerChunk = node.triangles.size / 3
            }
        }
        return maxTrisPerChunk
    }

    private fun checkOverlapRect(a: AABBf, b: Node): Boolean {
        return a.minX <= b.maxX && a.maxX >= b.minX &&
                a.minZ <= b.maxY && a.maxZ >= b.minY
    }

    fun getChunksOverlappingRect(bounds: AABBf): List<Node> {
        // Traverse tree
        val ids = ArrayList<Node>()
        var i = 0
        while (i < nodes.size) {
            val node = nodes[i]
            val overlap = checkOverlapRect(bounds, node)
            val isLeafNode = node.i >= 0
            if (isLeafNode && overlap) {
                ids.add(node)
            }
            if (overlap || isLeafNode) {
                i++
            } else {
                i = -node.i
            }
        }
        return ids
    }

    fun foreachChunkOverlappingRect(bounds: AABBf, callback: (Node) -> Unit) {
        // Traverse tree
        var i = 0
        while (i < nodes.size) {
            val node = nodes[i]
            val overlap = checkOverlapRect(bounds, node)
            val isLeafNode = node.i >= 0
            val x = isLeafNode && overlap
            if (x) callback(node)
            i = if (x) i + 1 else -node.i
        }
    }
}